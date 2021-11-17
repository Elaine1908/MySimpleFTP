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

public class RETRCommand extends AbstractCommand {
    public RETRCommand(String commandType, String commandArg) {
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

        //根据服务器的根目录，获得文件的绝对路径
        String fileAbsolutePath = new File(handleUserRequestThread.getRootPath() + File.separator + commandArg).getAbsolutePath();
        File file = new File(fileAbsolutePath);

        //处理找不到文件，或是应该是个文件，而实际是个目录的问题
        if (!file.exists()) {
            handleUserRequestThread.writeLine(new ArgumentWrongResponse("找不到文件！").toString());
            return;
        } else if (file.isDirectory()) {
            handleUserRequestThread.writeLine(new ArgumentWrongResponse("RETR的参数必须是一个文件，而不能是一个目录。如果你需要下载目录，应该先调用LFFR指令获得目录下所有的文件名，再由客户端循环调用RETR获取每个文件").toString());
            return;
        }

        //处理ASCII模式和KeepAlive=T的不兼容问题
        if (handleUserRequestThread.getAsciiBinary() == HandleUserRequestThread.ASCIIBinary.ASCII && handleUserRequestThread.getKeepAlive().equals("T")) {
            handleUserRequestThread.writeLine(new ArgumentWrongResponse("ASCII模式与持久数据连接不兼容").toString());
            return;
        }

        //写给一个用户命令OK的指令
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
            try (Scanner scanner = new Scanner(new File(fileAbsolutePath))) {

                //开Scanner
                OutputStream out = handleUserRequestThread.getDataSockets().get(0).getOutputStream();
                while (scanner.hasNext()) {//逐行输出
                    out.write(scanner.nextLine().getBytes(StandardCharsets.UTF_8));
                    out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }

                //写传输成功
                handleUserRequestThread.writeLine(new TransferSuccessResponse().toString());
            } catch (IOException e) {

                handleUserRequestThread.writeLine(new TransferFailedResponse().toString());
            } finally {
                //无论如何关闭连接
                handleUserRequestThread.getDataSockets().get(0).close();
                handleUserRequestThread.getDataSockets().clear();
            }


        } else {//BINARY模式

            // 创建对应的FileMeta对象
            FileMeta fileMeta = new FileMeta(file.length(), commandArg, FileMeta.Compressed.NOT_COMPRESSED);

            BufferedInputStream fileIn = null;

            try {
                OutputStream outputStream = handleUserRequestThread.getDataSockets().get(0).getOutputStream();

                //在数据连接上写FileMeta
                outputStream.write((JSON.toJSONString(fileMeta) + "\r\n").getBytes(StandardCharsets.UTF_8));

                //读取服务器硬盘上文件的缓冲字节流
                fileIn = new BufferedInputStream(new FileInputStream(fileAbsolutePath));
                //防止内存爆掉，每次只从带缓冲的字节流中读取1MB文件
                byte[] buf = new byte[1024 * 1024];
                while (true) {

                    //把文件读取到buf，记录读了多少字节
                    int bytesRead = fileIn.read(buf);
                    if (bytesRead == -1) {//如果是-1，说明流结束了
                        break;
                    }

                    //写这部分的文件到输出流上
                    outputStream.write(buf, 0, bytesRead);
                    outputStream.flush();
                }

                //写传输成功
                handleUserRequestThread.writeLine(new TransferSuccessResponse().toString());

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

                if (fileIn != null) {
                    fileIn.close();
                }
            }

        }


    }


}
