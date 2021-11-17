package core;

import com.alibaba.fastjson.JSON;
import core.exception.FTPClientException;
import core.exception.ServerNotFoundException;
import core.monitor.DownloadUploadProgressMonitor;
import core.monitor.data.DownloadUploadProgressData;
import core.transmit.FileMeta;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/**
 * FTP客户端的核心类
 */
public class MyFTPClientCore {

    private volatile String downloadDirectory;//ftp下载文件下载到哪个目录

    private final Socket commandSocket;//控制连接

    //串联在commandSocket上的reader和writer对象！
    private final BufferedWriter commandSocketWriter;
    private final BufferedReader commandSocketReader;


    /**
     * 记录客户端用的是主动模式还是被动模式
     */
    private enum PassiveActive {
        PASSIVE,
        ACTIVE
    }

    //默认使用被动模式
    private volatile PassiveActive passiveActive = PassiveActive.PASSIVE;
    private volatile String serverAddress;//服务器的地址，被动模式时RETR或者STOR需要连接
    private volatile int serverPort;//服务器的数据连接监听的端口，被动模式时RETR或者STOR需要连接
    private volatile ServerSocket serverSocket;//主动连接时，客户端要在这个socket上监听并accept！


    /**
     * 记录传输使用ASCII模式还是Binary模式！
     */
    public enum ASCIIBinary {
        ASCII, BINARY
    }

    private volatile ASCIIBinary asciiBinary = ASCIIBinary.BINARY;//默认使用Binary模式

    /**
     * 是否使用持久化数据连接
     */
    public enum KeepAlive {
        T,
        F
    }

    //默认不使用持久化数据连接！
    private volatile KeepAlive keepAlive = KeepAlive.F;

    //传输数据用的数据连接
    private volatile Socket dataSocket;

    private final List<DownloadUploadProgressMonitor> progressMonitors = new ArrayList<>();
    private volatile boolean monitorOpen = false;


    /**
     * 创建一个FTP客户端的对象
     * 连接主机名为host，端口为port的ftp服务器
     *
     * @param host FTP服务端的主机名
     * @param port FTP服务端的端口
     */
    public MyFTPClientCore(String host, int port) throws ServerNotFoundException {
        //尝试与Server建立控制连接！
        try {
            commandSocket = new Socket(host, port);
            commandSocketReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
            commandSocketWriter = new BufferedWriter(new OutputStreamWriter(commandSocket.getOutputStream()));
        } catch (IOException e) {
            throw new ServerNotFoundException(String.format("找不到位于(%s,%d)的服务器！", host, port));
        }
    }

    /**
     * 用username和password尝试登录这个FTP服务器，返回是否登录成功
     *
     * @param username 用户名
     * @param password 密码
     * @return 是否登录成功
     */
    public synchronized boolean login(String username, String password) {
        discardAllOnCommandConnection();
        //尝试登录
        try {

            //写USER指令，并读取处理响应
            writeLine(String.format("USER %s", username));
            String response = commandSocketReader.readLine();
            if (response.startsWith("230")) {//根据接口文档，230表示这个用户没有密码！
                return true;
            } else if (response.startsWith("331")) {//用户名OK，需要密码的情况

                //写PASS指令，并处理响应
                writeLine(String.format("PASS %s", password));
                response = commandSocketReader.readLine();
                //登录成功
                return response.startsWith("230");
            } else {
                return false;//其他情况都认为登录失败
            }
        } catch (IOException e) {//抛出异常说明登录失败
            discardAllOnCommandConnection();
            return false;
        }
    }


    /**
     * 像服务器发送PASV命令，并记录服务器返回的端口号和服务器主机名
     */
    public synchronized void pasv() throws FTPClientException {
        discardAllOnCommandConnection();
        try {
            //尝试写入PASV命令
            writeLine("PASV");

            //读取响应
            String response = commandSocketReader.readLine();

            //如果响应不是227开头的，说明失败
            if (!response.startsWith("227")) {
                discardAllOnCommandConnection();
                throw new FTPClientException(response);
            }

            //解析服务端的响应
            int a = response.indexOf("(");//获得开括弧的index
            int b = response.lastIndexOf(")");//获得闭括弧的index

            //获得开括弧和闭括弧的中间部分，就是server的端口和主机地址
            String serverAddressPort = response.substring(a + 1, b);
            String[] sArr = serverAddressPort.split(",");//按逗号分割类似于h1,h2,h3,h4,p1,p2

            //设置类对象的server_address和host
            serverAddress = sArr[0] + "." + sArr[1] + "." + sArr[2] + "." + sArr[3];
            serverPort = Integer.parseInt(sArr[4]) * 256 + Integer.parseInt(sArr[5]);

            //把主动还是被动设成PASSIVE
            passiveActive = PassiveActive.PASSIVE;
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }
    }

    /**
     *
     */
    public synchronized void port() throws FTPClientException {
        discardAllOnCommandConnection();
        try {
            //获得一个secureRandom对象，用于随机后面的端口
            SecureRandom random = new SecureRandom();

            boolean openServerSocketSuccess = false;

            //尝试随机打开一个ServerSocket
            do {
                //主动模式，随机在客户端的机器上打开一个端口监听！
                //先随机生成两个0~255的整数
                int p1 = random.nextInt(256);
                int p2 = random.nextInt(256);

                //获得端口
                int port = p1 * 256 + p2;


                //设置ServerSocket监听服务器端传来的连接
                ServerSocket serverSocket = null;
                try {
                    serverSocket = new ServerSocket(port);
                } catch (IOException e) {//由于可能端口冲突之类的问题，打开ServerSocket可能会抛出异常，因此如果抛出了异常就再试试一次
                    continue;
                }

                //设置用来跳出循环的变量
                openServerSocketSuccess = true;

                //获取本机的地址
                String localAddress = Objects.requireNonNull(Utils.getLocalHostExactAddress()).getHostAddress();

                //写给Server端的请求
                String line = String.format("PORT %s,%d,%d", localAddress.replace(".", ","), p1, p2);

                //把请求写给server
                writeLine(line);

                String response = commandSocketReader.readLine();
                if (!response.startsWith("200")) {
                    discardAllOnCommandConnection();
                    throw new FTPClientException(response);
                }

                //如果原来有打开主动模式的数据连接监听端口，就把他关掉
                if (this.serverSocket != null) {
                    this.serverSocket.close();
                }

                //将传输模式设置成主动模式
                passiveActive = PassiveActive.ACTIVE;
                this.serverSocket = serverSocket;


            } while (!openServerSocketSuccess);


        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }

    }

    public synchronized void type(ASCIIBinary asciiBinary) throws FTPClientException {
        discardAllOnCommandConnection();
        //B代表Binary模式，A代表ASCII模式！
        String arg = (asciiBinary == ASCIIBinary.BINARY) ? "B" : "A";

        //要写给服务器的line
        String line = String.format("TYPE %s", arg);

        //写给服务器
        try {
            writeLine(line);
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }

        //读取服务器响应
        String response;
        try {
            response = commandSocketReader.readLine();
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }

        //如果服务器传来的响应不成功
        if (response == null || !response.startsWith("200")) {
            discardAllOnCommandConnection();
            throw new FTPClientException(response);
        }

        //否则，说明服务器设置成功，设置本地是用ASCII还是Binary存储
        this.asciiBinary = asciiBinary;
    }

    public synchronized void kali(KeepAlive keepAlive) throws FTPClientException {
        discardAllOnCommandConnection();
        //T代表使用持久化数据连接，F代表不适用持久化数据连接！
        String arg = (keepAlive == KeepAlive.F) ? "F" : "T";

        //要写给服务器的line
        String line = String.format("KALI %s", arg);

        //写给服务器
        try {
            writeLine(line);
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }

        //读取服务器响应
        String response;
        try {
            response = commandSocketReader.readLine();
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }

        //如果服务器传来的响应不成功
        if (response == null || !response.startsWith("200")) {
            discardAllOnCommandConnection();
            throw new FTPClientException(response);
        }

        //否则，说明服务器设置成功，设置本地是否使用持久化连接！
        this.keepAlive = keepAlive;
    }

    /**
     * 列出服务器上名为folderName的文件夹内的所有文件
     *
     * @param folderName 文件夹名
     * @return 文件名的list
     */
    public synchronized List<String> lffr(String folderName) throws FTPClientException {
        discardAllOnCommandConnection();
        //要写给服务器的行
        String line = String.format("LFFR %s", folderName);

        try {
            //把行写给服务器
            writeLine(line);
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }

        //读取服务器的请求
        String response;
        try {
            response = commandSocketReader.readLine();
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }

        if (response == null || !response.startsWith("200")) {
            discardAllOnCommandConnection();
            throw new FTPClientException(response);
        }

        List<String> filenameList = new ArrayList<>();

        //循环读取控制连接上传来的文件信息
        while (true) {
            String filenameInfo;
            try {
                filenameInfo = commandSocketReader.readLine();
            } catch (IOException e) {
                continue;
            }
            if (filenameInfo == null || filenameInfo.length() == 0) {
                break;
            }
            filenameList.add(filenameInfo);
        }

        return filenameList;
    }

    /**
     * 向服务器发送RETR命令 获取一个文件夹
     *
     * @param arg RETR命令的参数
     */
    public synchronized void retrieveFolder(String arg) throws FTPClientException {
        try {
            String[] arr = arg.split("[/]|[\\\\]");

            //文件夹的名字
            String folderName = arr[arr.length - 1];

            //先用LFFR指令获得所有文件的列表
            List<String> serverFilenameList = lffr(arg);

            //计算出每个文件在client上存放的物理位置
            List<String> clientFilenameList = Utils.getClientFilenames(downloadDirectory, serverFilenameList, folderName, arg);

            //创建必要的文件夹
            Utils.createFolders(clientFilenameList);

            //从服务器上一一下载文件
            int i = 0;
            int j = 0;
            while (i < serverFilenameList.size() && j < clientFilenameList.size()) {
                retrieveSingleFileHidden(serverFilenameList.get(i), clientFilenameList.get(j));
                i++;
                j++;
            }

        } catch (Exception e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }
    }

    /**
     * 向服务器发送RETR命令 获取一个文件
     *
     * @param arg RETR命令的参数
     */
    public synchronized void retrieveSingleFile(String arg) throws FTPClientException {
        if (downloadDirectory == null) {
            throw new FTPClientException("没有设定下载文件的目录");
        }

        String[] arr = arg.split("[/]|[\\\\]");//根据路径分隔符分割服务器上的绝对路径
        String filename = arr[arr.length - 1];//文件真正的文件名

        //要下载到的文件的文件名
        String destFilename = downloadDirectory + File.separator + filename;

        retrieveSingleFileHidden(arg, destFilename);
    }

    /**
     * 根据文件名，下载服务器上的一个文件到指定的文件名上。
     *
     * @param arg 文件名
     * @throws FTPClientException 异常
     */
    private synchronized void retrieveSingleFileHidden(String arg, String destFilename) throws FTPClientException {
        discardAllOnCommandConnection();
        //先在控制连接上写想要获取这个文件的命令！
        try {
            writeLine(String.format("RETR %s", arg));
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }

        //先读取服务端的响应是不是200OK
        try {
            String response = commandSocketReader.readLine();
            if (!response.startsWith("200")) {
                discardAllOnCommandConnection();
                throw new FTPClientException(response);
            }
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }


        //如果不使用持久连接模式，或者使用持久连接模式，但是还从来没有建立过数据连接，就要尝试新建数据连接
        if (keepAlive == KeepAlive.F || (keepAlive == KeepAlive.T && dataSocket == null)) {
            boolean success = buildDataConnection();
            if (!success) {
                discardAllOnCommandConnection();
                throw new FTPClientException("建立数据连接失败");
            }
        }

        //先读取服务端的响应是不是125已经建立数据连接
        try {
            String response = commandSocketReader.readLine();
            if (!response.startsWith("125")) {
                discardAllOnCommandConnection();
                throw new FTPClientException(response);
            }
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }


        //准备从dataSocket上开始读取了
        if (asciiBinary == ASCIIBinary.ASCII) {//ASCII模式，注意这个模式应该只用于文本文件，否则下载到文件的MD5可能不一致。
            try {
                BufferedWriter fOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFilename)));

                //获得数据连接的reader，逐行读取并写入文件
                BufferedReader reader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));

                //为了防止文件最后多一个换行符的问题，我将newline放在写一新的一行的前面，而不是后面。因此需要一个变量记录已经读了几行，因为第0行开始是不需要newline的。
                int lineRead = 0;

                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (lineRead > 0) {
                        fOut.newLine();
                    }
                    fOut.write(line);
                    lineRead++;
                }

                //关闭数据连接与文件流
                dataSocket.close();
                dataSocket = null;
                fOut.close();

            } catch (IOException e) {
                throw new FTPClientException(e.getMessage());
            }

        } else {//Binary模式
            try {
                //获得文件输出流
                BufferedOutputStream fOut = new BufferedOutputStream(new FileOutputStream(destFilename));

                //从server上读取字节流的缓冲区，防止内存爆掉，每次最多读取1MB！
                byte[] buf = new byte[1024 * 1024];

                //从数据连接获得inputstream
                InputStream in = dataSocket.getInputStream();

                //读取filemeta
                FileMeta fileMeta = JSON.parseObject(Utils.readline(in), FileMeta.class);

                //总共已经读取并写入了多少bytes
                int totalBytesRead = 0;

                while (true) {

                    //这一次读取了多少bytes
                    int thisBytesRead = in.read(buf);

                    totalBytesRead += thisBytesRead;

                    if (totalBytesRead < fileMeta.size) {
                        fOut.write(buf, 0, thisBytesRead);
                    } else if (totalBytesRead >= fileMeta.size) {
                        fOut.write(buf, 0, thisBytesRead);
                        break;
                    }

                    if (monitorOpen) {
                        //通知监视器以显示进度
                        notifyAllProgressMonitors(new DownloadUploadProgressData(fileMeta.filename, fileMeta.size, totalBytesRead, DownloadUploadProgressData.Type.DOWNLOAD));
                    }


                }

                //关闭文件输出流
                fOut.close();

                //如果是非持久连接则关闭
                if (keepAlive == KeepAlive.F) {
                    dataSocket.close();
                    dataSocket = null;
                }

            } catch (IOException e) {
                throw new FTPClientException(e.getMessage());
            }
        }

        //读取最后的响应
        try {
            String response = commandSocketReader.readLine();
            //检测文件是否传输成功
            if (!response.startsWith("226")) {
                discardAllOnCommandConnection();
                throw new FTPClientException(response);
            }
        } catch (IOException e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }
    }


    /**
     * 调用STOR指令，上传单一的文件到服务器
     *
     * @param arg                     STOR指令的参数
     * @param filePathOnClientMachine 上传本机的哪个文件
     * @throws FTPClientException 异常。。。。。
     */
    private synchronized void storeSingleFileHidden(String arg, String filePathOnClientMachine) throws FTPClientException {
        discardAllOnCommandConnection();
        try {
            //向数据连接写STOR的请求
            writeLine(String.format("STOR %s", arg));
            String response = commandSocketReader.readLine();

            //如果服务器的响应不是200，比如存在一些没有指定主动被动模式，未登录等问题，就抛出异常
            if (!response.startsWith("200")) {
                throw new FTPClientException(response);
            }

            //尝试建立数据连接
            //如果不使用持久连接模式，或者使用持久连接模式，但是还从来没有建立过数据连接，就要尝试新建数据连接
            if (keepAlive == KeepAlive.F || (keepAlive == KeepAlive.T && dataSocket == null)) {
                boolean success = buildDataConnection();
                if (!success) {
                    throw new FTPClientException("建立数据连接失败");
                }
            }

            //检查服务器发来的响应里面数据连接是否已经打开，如果连接打开失败一类的，就抛出异常
            response = commandSocketReader.readLine();
            if (!response.startsWith("125")) {
                throw new FTPClientException(response);
            }

            //准备开始传输
            if (asciiBinary == ASCIIBinary.ASCII) {//ASCII模式
                try (Scanner scanner = new Scanner(new FileInputStream(filePathOnClientMachine))) {
                    //用Scanner打开要传输的文件，打开数据连接的outputstream
                    OutputStream outputStream = dataSocket.getOutputStream();

                    //不断用scanner读取行，写到BufferedWriter上面
                    while (scanner.hasNext()) {
                        String line = scanner.nextLine();
                        outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();
                    }
                } catch (IOException ignored) {//传输失败
                } finally {//在finally里面关闭数据连接
                    dataSocket.close();
                    dataSocket = null;
                }

                response = commandSocketReader.readLine();
                if (!response.startsWith("226")) {
                    throw new FTPClientException(response);
                }


            } else {//Binary模式

                //开启文件输入流，并自动关闭
                try (BufferedInputStream fIn = new BufferedInputStream(new FileInputStream(filePathOnClientMachine))) {

                    //创建一个fileMeta对象
                    FileMeta fileMeta = new FileMeta(new File(filePathOnClientMachine).length(), arg, FileMeta.Compressed.NOT_COMPRESSED);

                    //获得数据连接的输出流
                    OutputStream outputStream = dataSocket.getOutputStream();

                    //在数据连接上写fileMeta
                    outputStream.write((JSON.toJSONString(fileMeta) + "\r\n").getBytes(StandardCharsets.UTF_8));

                    //防止内存爆掉，每次都只写1MB
                    byte[] buf = new byte[1024 * 1024];

                    //总共写了多少字节
                    int totalBytesWritten = 0;
                    while (true) {
                        //把文件读取到buf，记录读了多少字节
                        int bytesRead = fIn.read(buf);
                        if (bytesRead == -1) {//如果是-1，说明流结束了
                            break;
                        }

                        //写这部分的文件到输出流上
                        outputStream.write(buf, 0, bytesRead);
                        outputStream.flush();

                        totalBytesWritten += bytesRead;

                        if (monitorOpen){
                            //通知监视器以显示上传进度
                            notifyAllProgressMonitors(new DownloadUploadProgressData(filePathOnClientMachine, fileMeta.size, totalBytesWritten, DownloadUploadProgressData.Type.UPLOAD));

                        }
                    }
                } catch (IOException e) {//传输失败，关闭数据连接
                    dataSocket.close();
                    dataSocket = null;
                }

                //如果是非持久模式就关闭连接
                if (keepAlive == KeepAlive.F) {
                    dataSocket.close();
                    dataSocket = null;
                }

                response = commandSocketReader.readLine();
                if (!response.startsWith("226")) {
                    throw new FTPClientException(response);
                }

            }

        } catch (Exception e) {
            discardAllOnCommandConnection();
            throw new FTPClientException(e.getMessage());
        }
    }

    /**
     * 调用STOR指令，上传一个本机上的文件到服务器
     *
     * @param filenameOnClientMachine 本机上的文件名
     * @param serverFolder            上传到服务器的哪个文件夹
     */
    public synchronized void storeSingleFile(String filenameOnClientMachine, String serverFolder) throws FTPClientException {
        File file = new File(filenameOnClientMachine);
        if (file.isDirectory()) {
            throw new FTPClientException("不是文件");
        }
        String simpleFilename = file.getName();
        storeSingleFileHidden(serverFolder + "/" + simpleFilename, file.getAbsolutePath());
    }

    public synchronized void storeSingleFile(String filenameOnClientMachine) throws FTPClientException {
        storeSingleFile(filenameOnClientMachine, "/");
    }

    public synchronized void storeFolder(String folderNameOnClientMachine, String serverFolder) throws FTPClientException {
        //获得客户机上的文件夹里面所有文件的绝对路径
        List<String> clientFilenameList = new FilenameGetter().getAllFilenames(folderNameOnClientMachine);

        //获得服务器上的文件名列表
        List<String> serverFilenameList = Utils.getServerFilenames(serverFolder, clientFilenameList, folderNameOnClientMachine);

        int i = 0;
        int j = 0;
        while (i < clientFilenameList.size() && j < serverFilenameList.size()) {
            storeSingleFileHidden(serverFilenameList.get(j), clientFilenameList.get(i));

            i++;
            j++;
        }

    }

    public synchronized void storeFolder(String folderNameOnClientMachine) throws FTPClientException {
        storeFolder(folderNameOnClientMachine, "/");
    }


    /**
     * 与服务器建立数据连接
     *
     * @return 建立连接是否成功
     */
    public boolean buildDataConnection() throws FTPClientException {

        //关闭原先的数据连接
        try {
            if (dataSocket != null) {
                dataSocket.close();
            }
        } catch (IOException ignored) {
        }


        if (passiveActive == PassiveActive.ACTIVE) {//主动模式，由客户端来ACCEPT
            try {
                this.dataSocket = serverSocket.accept();
                return true;
            } catch (IOException e) {
                discardAllOnCommandConnection();
                throw new FTPClientException(e.getMessage());
            }

        } else {//被动模式
            try {
                this.dataSocket = new Socket(serverAddress, serverPort);
                return true;
            } catch (IOException e) {
                discardAllOnCommandConnection();
                throw new FTPClientException(e.getMessage());
            }
        }

    }


    /**
     * 设置文件下载到哪个目录
     *
     * @param downloadDirectory 文件下载到哪个目录！
     */
    public synchronized void setDownloadDirectory(String downloadDirectory) throws FTPClientException {
        File dir = new File(downloadDirectory);
        if (!dir.exists()) {
            throw new FTPClientException("目录不存在！");
        }
        if (!dir.isDirectory()) {
            throw new FTPClientException("不是目录，请选择一个目录！");
        }
        this.downloadDirectory = downloadDirectory;
    }


    public String getDownloadDirectory() {
        return downloadDirectory;
    }

    /**
     * 在控制连接上写入一行
     *
     * @param line 字符串
     * @throws IOException 如果写入错误，比如控制连接已经关闭，就抛出异常！
     */
    private void writeLine(String line) throws IOException {
        commandSocketWriter.write(line);
        commandSocketWriter.write("\r\n");
        commandSocketWriter.flush();
    }

    private void discardAllOnCommandConnection() {
        try {
            while (commandSocketReader.ready()) {
                commandSocketReader.read();
            }
        } catch (IOException e) {
        }
    }

    /**
     * 添加一个进度监视器
     *
     * @param progressMonitor 进度监视器
     */
    public void addProgressMonitor(DownloadUploadProgressMonitor progressMonitor) {
        this.progressMonitors.add(progressMonitor);
    }

    /**
     * 通知所有的监视器
     *
     * @param data 通知用的数据
     */
    public void notifyAllProgressMonitors(DownloadUploadProgressData data) {
        progressMonitors.forEach(progressMonitor -> progressMonitor.notify(data));
    }

    public boolean isMonitorOpen() {
        return monitorOpen;
    }

    public void setMonitorOpen(boolean monitorOpen) {
        this.monitorOpen = monitorOpen;
    }
}

class Utils {
    public static String readline(InputStream in) throws IOException {
        List<Byte> bytesList = new ArrayList<>();
        boolean streamClosed = false;
        while (true) {
            int x = in.read();
            //流结束了也应该bread
            if (x == -1) {
                streamClosed = true;
                break;
            }
            byte b = (byte) x;
            if (b == '\n') {
                break;
            }
            bytesList.add(b);
        }
        if (streamClosed) {//如果流终止了，就抛出异常
            throw new IOException();
        }
        if (bytesList.get(bytesList.size() - 1) == '\r') {
            bytesList.remove(bytesList.size() - 1);
        }

        byte[] byteArr = new byte[bytesList.size()];
        for (int i = 0; i < byteArr.length; i++) {
            byteArr[i] = bytesList.get(i);
        }

        return new String(byteArr);
    }

    /**
     * 在下载文件时，根据LFFR指令获得的服务器上文件名列表，再加上要下载到哪个目录，和要下载的文件夹名，和RETR指令的参数，给出客户机上每个下载文件的绝对路径！
     *
     * @param downloadDirectory  下载目录
     * @param serverFilenameList 服务器上文件名的列表
     * @param folderName         要下载的文件夹的名字
     * @return 客户机上每个下载文件存储的绝对路径
     */
    public static List<String> getClientFilenames(String downloadDirectory, List<String> serverFilenameList, String folderName, String retrArg) {
        List<String> clientFilenameList = new ArrayList<>();
        for (String serverFilename : serverFilenameList
        ) {
            String clientFilename =
                    downloadDirectory +
                            File.separator +
                            folderName +
                            File.separator +
                            String.join(File.separator, removePrefix(retrArg.split("[/]|[\\\\]"), serverFilename.split("[/]|[\\\\]")));
            clientFilenameList.add(clientFilename);

        }
        return clientFilenameList;
    }

    /**
     * 在使用STOR上传文件夹时，根据上传到服务器的目标路径，客户机上每个文件的绝对路径，和客户端要上传的文件夹的绝对路径，获得服务器上每个文件的路径
     *
     * @param serverDirectory             要上传到服务器哪个文件夹
     * @param clientFilenameList          文件夹中的每个文件在客户端中的绝对路径
     * @param clientDirectoryAbsolutePath 要上传的文件夹在客户端中的绝对路径
     * @return 服务器上每个文件应该在哪个路径
     */
    public static List<String> getServerFilenames(String serverDirectory, List<String> clientFilenameList, String clientDirectoryAbsolutePath) {
        List<String> serverFilenameList = new ArrayList<>();
        String folderName = new File(clientDirectoryAbsolutePath).getName();
        for (String clientFilename : clientFilenameList
        ) {

            String serverFilename =
                    serverDirectory +
                            "/" +
                            folderName +
                            "/" +
                            String.join("/", removePrefix(clientDirectoryAbsolutePath.split("[/]|[\\\\]"), clientFilename.split("[/]|[\\\\]")));
            serverFilenameList.add(serverFilename);

        }
        return serverFilenameList;
    }

    /**
     * 给strArr移除掉prefix的前缀，作为list返回
     *
     * @param prefix 前缀数组
     * @param strArr 要操作的字符串数组
     * @return 去除前缀完成后，以list形式返回
     */
    private static List<String> removePrefix(String[] prefix, String[] strArr) {
        int i = 0;
        int j = 0;
        while (i < prefix.length && j < strArr.length) {
            if (prefix[i].length() == 0) {
                i++;
                continue;
            }
            if (strArr[j].length() == 0) {
                j++;
                continue;
            }
            if (Objects.equals(prefix[i], strArr[j])) {
                i++;
                j++;
            } else {
                break;
            }
        }
        List<String> res = new ArrayList<>();
        while (j < strArr.length) {
            res.add(strArr[j]);
            j++;
        }
        return res;
    }

    public static void createFolders(List<String> clientFilenameList) {

        for (String clientFilename : clientFilenameList) {
            File file = new File(clientFilename).getAbsoluteFile();
            File parent = file.getParentFile();
            if (parent != null) {
                boolean success = parent.mkdirs();
            }
        }
    }

    public static InetAddress getLocalHostExactAddress() {
        try {
            InetAddress candidateAddress = null;

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                // 该网卡接口下的ip会有多个，也需要一个个的遍历，找到自己所需要的
                for (Enumeration<InetAddress> inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = inetAddrs.nextElement();
                    // 排除loopback回环类型地址（不管是IPv4还是IPv6 只要是回环地址都会返回true）
                    if (!inetAddr.isLoopbackAddress()) {
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了 就是我们要找的
                            // ~~~~~~~~~~~~~绝大部分情况下都会在此处返回你的ip地址值~~~~~~~~~~~~~
                            return inetAddr;
                        }

                        // 若不是site-local地址 那就记录下该地址当作候选
                        if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }

                    }
                }
            }

            // 如果出去loopback回环地之外无其它地址了，那就回退到原始方案吧
            return candidateAddress == null ? InetAddress.getLocalHost() : candidateAddress;
        } catch (Exception e) {
            return null;
        }
    }


}

class FilenameGetter {
    private final List<String> folderNameList;

    public FilenameGetter() {
        folderNameList = new ArrayList<>();
    }

    /**
     * 获取一个目录下的所有文件的文件名
     *
     * @param folderPath 目录的（绝对）路径
     * @return 目录下所有文件的文件名的绝对路径
     */
    public List<String> getAllFilenames(String folderPath) {
        folderNameList.clear();
        File folder = new File(folderPath);
        if (!folder.isDirectory()) {
            throw new IllegalArgumentException("不是目录！");
        }
        backtrack(folder);

        return folderNameList;
    }

    public void backtrack(File folder) {
        File[] subFiles = folder.listFiles();
        assert subFiles != null;
        for (File subFile : subFiles) {
            if (subFile.isDirectory()) {
                backtrack(subFile);
            } else {
                folderNameList.add(subFile.getAbsolutePath());
            }
        }
    }
}

