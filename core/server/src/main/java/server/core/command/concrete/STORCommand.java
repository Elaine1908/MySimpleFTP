package server.core.command.concrete;

import com.alibaba.fastjson.JSON;
import server.core.command.AbstractCommand;
import server.core.response.concrete.*;
import server.core.thread.HandleUserRequestThread;
import server.core.transmit.FileMeta;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class STORCommand extends AbstractCommand {

    public STORCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {

        //处理未登录的问题
        if (!handleUserRequestThread.isLoginSuccessful()) {
            handleUserRequestThread.writeLine(new NotLoginResponse().toString());
            return;
        }

        //处理没有指定PASV还是PORT的问题
        if (handleUserRequestThread.getPassiveActive() == null) {
            handleUserRequestThread.writeLine(new BadCommandSequenceResponse().toString());
            return;
        }

        //处理ASCII和Keep-Alive=T的不兼容问题
        if (handleUserRequestThread.getAsciiBinary() == HandleUserRequestThread.ASCIIBinary.ASCII && handleUserRequestThread.getKeepAlive().equals("T")) {
            handleUserRequestThread.writeLine(new ArgumentWrongResponse("ASCII模式与持久数据连接不兼容").toString());
            return;
        }

        //获得要保存到server上的哪个文件的绝对路径
        String absoluteFilepath = handleUserRequestThread.getRootPath() + File.separator + commandArg;

        //创建存储这个文件所需要的路径
        Utils.createFolders(absoluteFilepath);

        //写给客户端一个200-OK的指令
        handleUserRequestThread.writeLine(new CommandOKResponse().toString());

        //准备建立数据连接，先看看是不是keep-alive，如果是的话就不用建立连接了。
        if (
                "F".equals(handleUserRequestThread.getKeepAlive()) ||
                        ("T".equals(handleUserRequestThread.getKeepAlive()) && handleUserRequestThread.getDataSockets().size() == 0)) {//如果不是是采用持久连接，或者采用了持久连接，但是还没有打开过数据连接，则要建立一个数据连接。。。。
            boolean success = handleUserRequestThread.buildDataConnection();
            if (success) {//建立连接成功与失败，分别写给用户响应
                handleUserRequestThread.writeLine(new ConnectionAlreadyOpenResponse().toString());
            } else {
                handleUserRequestThread.writeLine(new OpenDataConnectionFailedResponse().toString());
                return;
            }
        } else {
            handleUserRequestThread.writeLine(new ConnectionAlreadyOpenResponse().toString());
        }

        //准备写数据！
        if (handleUserRequestThread.getAsciiBinary() == HandleUserRequestThread.ASCIIBinary.ASCII) {
            //ASCII模式，强制非持久化连接！
            try {
                //把数据连接的socket的inputstream串联到一个dataReader上
                BufferedReader dataReader = new BufferedReader(new InputStreamReader(handleUserRequestThread.getDataSockets().get(0).getInputStream()));
                //创建要保存到文件的bufferedWriter
                BufferedWriter fOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(absoluteFilepath)));

                //现在已经从数据连接上读取了几行
                int lineRead = 0;

                //从数据连接上一行行读取，写到磁盘的文件中
                while (true) {
                    String line = dataReader.readLine();
                    if (line == null) {
                        break;
                    }
                    if (lineRead > 0) {
                        fOut.newLine();
                    }
                    lineRead++;
                    fOut.write(line);
                }

                //写传输成功
                handleUserRequestThread.writeLine(new TransferSuccessResponse().toString());

                //关闭写文件流
                fOut.close();
            } catch (IOException e) {
                handleUserRequestThread.writeLine(new TransferFailedResponse().toString());
            } finally {
                //无论如何关闭连接
                handleUserRequestThread.getDataSockets().get(0).close();
                handleUserRequestThread.getDataSockets().clear();
            }


        } else {//BINARY模式
            try {
                //数据连接上的输入流
                InputStream dataSocketInputStream = handleUserRequestThread.getDataSockets().get(0).getInputStream();

                //要保存的文件的输出流
                OutputStream fOut = new BufferedOutputStream(new FileOutputStream(absoluteFilepath));

                //从数据连接上读取一行，反序列化成filemeta
                FileMeta fileMeta = JSON.parseObject(Utils.readline(dataSocketInputStream), FileMeta.class);

                //防止内存爆掉，每次只从带缓冲的字节流中读取1MB文件
                byte[] buf = new byte[1024 * 1024];

                //总共读取了多少字节
                int totalBytesRead = 0;

                while (true) {
                    //这次读取了多少字节
                    int thisBytesRead = dataSocketInputStream.read(buf);
                    totalBytesRead += thisBytesRead;
                    if (totalBytesRead < fileMeta.size) {//还没读完，还有
                        fOut.write(buf, 0, thisBytesRead);
                    } else if (totalBytesRead >= fileMeta.size) {//读完了，没有了，就break
                        fOut.write(buf, 0, thisBytesRead);
                        break;
                    }
                }

                //写传输成功
                handleUserRequestThread.writeLine(new TransferSuccessResponse().toString());

                //关闭文件流
                fOut.close();

                //根据是不是keep-alive决定要不要关闭数据连接
                if ("F".equals(handleUserRequestThread.getKeepAlive())) {
                    try {
                        handleUserRequestThread.getDataSockets().get(0).close();
                        handleUserRequestThread.getDataSockets().clear();
                    } catch (IOException ignored) {
                    }
                }
            } catch (IOException e) {

                handleUserRequestThread.writeLine(new TransferFailedResponse().toString());

                //如果传输失败，无论如何关闭数据连接
                handleUserRequestThread.getDataSockets().get(0).close();
                handleUserRequestThread.getDataSockets().clear();
            }

        }
    }

}

class Utils {
    public static void createFolders(String absoluteFilepath) {
        //将文件最后的文件名去掉
        File file = new File(absoluteFilepath);
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            boolean success = parentFile.mkdirs();
        }

    }

    public static String readline(InputStream in) throws IOException {
        List<Byte> bytesList = new ArrayList<>();
        while (true) {
            byte b = (byte) in.read();
            if (b == '\n') {
                break;
            }
            bytesList.add(b);
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

}
