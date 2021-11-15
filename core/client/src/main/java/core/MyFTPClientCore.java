package core;

import core.exception.FTPClientException;
import core.exception.ServerNotFoundException;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * FTP客户端的核心类
 */
public class MyFTPClientCore {

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
    private PassiveActive passiveActive = PassiveActive.PASSIVE;
    private String serverAddress;//服务器的地址，被动模式时RETR或者STOR需要连接
    private int serverPort;//服务器的数据连接监听的端口，被动模式时RETR或者STOR需要连接
    private ServerSocket serverSocket;//主动连接时，客户端要在这个socket上监听并accept！


    /**
     * 记录传输使用ASCII模式还是Binary模式！
     */
    public enum ASCIIBinary {
        ASCII, BINARY
    }

    private ASCIIBinary asciiBinary = ASCIIBinary.BINARY;//默认使用Binary模式

    /**
     * 是否使用持久化数据连接
     */
    public enum KeepAlive {
        T,
        F
    }

    //默认不使用持久化数据连接！
    private KeepAlive keepAlive = KeepAlive.F;


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
    public boolean login(String username, String password) {
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
            return false;
        }
    }


    /**
     * 像服务器发送PASV命令，并记录服务器返回的端口号和服务器主机名
     */
    public void pasv() throws FTPClientException {

        try {
            //尝试写入PASV命令
            writeLine("PASV");

            //读取响应
            String response = commandSocketReader.readLine();

            //如果响应不是227开头的，说明失败
            if (!response.startsWith("227")) {
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
            throw new FTPClientException(e.getMessage());
        }
    }

    /**
     *
     */
    public void port() throws FTPClientException {
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
                String localAddress = InetAddress.getLocalHost().getHostAddress();

                //写给Server端的请求
                String line = String.format("PORT %s,%d,%d", localAddress.replace(".", ","), p1, p2);

                //把请求写给server
                writeLine(line);

                String response = commandSocketReader.readLine();
                if (!response.startsWith("200")) {
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
            throw new FTPClientException(e.getMessage());
        }

    }

    public void type(ASCIIBinary asciiBinary) throws FTPClientException {

        //B代表Binary模式，A代表ASCII模式！
        String arg = (asciiBinary == ASCIIBinary.BINARY) ? "B" : "A";

        //要写给服务器的line
        String line = String.format("TYPE %s", arg);

        //写给服务器
        try {
            writeLine(line);
        } catch (IOException e) {
            throw new FTPClientException(e.getMessage());
        }

        //读取服务器响应
        String response;
        try {
            response = commandSocketReader.readLine();
        } catch (IOException e) {
            throw new FTPClientException(e.getMessage());
        }

        //如果服务器传来的响应不成功
        if (response == null || !response.startsWith("200")) {
            throw new FTPClientException(response);
        }

        //否则，说明服务器设置成功，设置本地是用ASCII还是Binary存储
        this.asciiBinary = asciiBinary;
    }

    public void kali(KeepAlive keepAlive) throws FTPClientException {
        //T代表使用持久化数据连接，F代表不适用持久化数据连接！
        String arg = (keepAlive == KeepAlive.F) ? "F" : "T";

        //要写给服务器的line
        String line = String.format("KALI %s", arg);

        //写给服务器
        try {
            writeLine(line);
        } catch (IOException e) {
            throw new FTPClientException(e.getMessage());
        }

        //读取服务器响应
        String response;
        try {
            response = commandSocketReader.readLine();
        } catch (IOException e) {
            throw new FTPClientException(e.getMessage());
        }

        //如果服务器传来的响应不成功
        if (response == null || !response.startsWith("200")) {
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
    public List<String> lffr(String folderName) throws FTPClientException {
        //要写给服务器的行
        String line = String.format("LFFR %s", folderName);

        try {
            //把行写给服务器
            writeLine(line);
        } catch (IOException e) {
            throw new FTPClientException(e.getMessage());
        }

        //读取服务器的请求
        String response;
        try {
            response = commandSocketReader.readLine();
        } catch (IOException e) {
            throw new FTPClientException(e.getMessage());
        }

        if (response == null || !response.startsWith("200")) {
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

}
