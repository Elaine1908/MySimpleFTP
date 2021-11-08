package server.core.thread;

import com.alibaba.fastjson.JSON;
import org.apache.log4j.Logger;
import server.core.command.AbstractCommand;
import server.core.command.factory.CommandFactory;
import server.core.exception.CommandSyntaxWrongException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 此时FTP服务器已经accept了用户的连接请求，建立了控制连接，则新建此类的线程，专门用于处理用户的USER,PASS,STOR,RETR等命令
 */
public class HandleUserRequestThread extends Thread {

    /**
     * 此时是主动传输模式还是被动传输模式
     */
    public enum PassiveActive {
        PASSIVE, ACTIVE
    }

    private final Socket commandSocket;//和用户的控制连接，用户在控制连接上输入指令，然后服务端在控制连接上读取并解析用户的指令

    private final BufferedReader commandConnReader;//在控制连接上读取的字符流

    private final BufferedWriter commandConnWriter;//在控制连接上写入的字符流

    private final List<Socket> dataSockets;//和用户的数据连接（们），因为日后可能需要使用多个Socket，将一个大文件分开进行并发传输。

    private boolean loginSuccessful;//当前用户是否已经登录成功

    private String username;//当前用户的用户名

    public final Map<String, String> usernameToPassword;

    private final Logger logger = Logger.getLogger(HandleUserRequestThread.class);//日志记录器

    private PassiveActive passiveActive;//记录此时是被动模式还是主动模式

    private ServerSocket passiveModeServerSocket;//被动模式下用来监听用户连接请求的socket

    private String clientIPAddress;//用于在主动模式中记录客户端的ip地址

    private int clientPort;//用于在主动模式下记录客户端的端口


    /**
     * 在已经accept了用户的连接请求，获得了控制连接后，新建一个处理用户请求的线程！（但不启动它）
     *
     * @param commandSocket 与用户的控制连接
     */
    public HandleUserRequestThread(Socket commandSocket) throws IOException {
        //BasicConfigurator.configure();

        this.commandSocket = commandSocket;

        commandConnReader = new BufferedReader(new InputStreamReader(commandSocket.getInputStream()));
        commandConnWriter = new BufferedWriter(new OutputStreamWriter(commandSocket.getOutputStream()));

        //创建一个数据连接的列表，现在列表里没有任何数据连接，等待用户有用PORT或PASV后，便有了数据连接
        dataSockets = new ArrayList<>();

        //用户名和密码，这里就写死了
        usernameToPassword = new HashMap<>();
        usernameToPassword.put("anonymous", null);
        usernameToPassword.put("test", "test");
    }


    @Override
    public void run() {

        //这个线程无线循环，监听用户的请求
        while (true) {
            try {
                //读取一行用户输入
                String commandLine = commandConnReader.readLine();

                //如果这里是null，就抛出IO异常。
                if (commandLine == null) {
                    throw new IOException();
                }

                //解析用户输入，获得命令对象
                AbstractCommand command = CommandFactory.parseCommand(commandLine);

                logger.info(String.format("已收到来自%s的请求%s，准备开始执行%n", commandSocket.getRemoteSocketAddress().toString(), JSON.toJSONString(command)));

                command.execute(this);

            } catch (CommandSyntaxWrongException commandSyntaxWrongException) {
                try {
                    writeLine(commandSyntaxWrongException.toString());
                } catch (IOException ignored) {
                }
            } catch (IOException e) {//如果运行到这里，说明连接终端了
                closeAllConnections();
                logger.info(String.format("%s已退出", commandSocket));
                break;
            }
        }

    }

    public void writeLine(String line) throws IOException {
        //如果解析命令的过程中出错，就写给用户错误信息
        commandConnWriter.write(line);
        commandConnWriter.write("\r\n");
        commandConnWriter.flush();

    }

    private void closeAllConnections() {
        try {
            commandSocket.close();
            for (Socket socket : dataSockets
            ) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

    }

    public Socket getCommandSocket() {
        return commandSocket;
    }

    public List<Socket> getDataSockets() {
        return dataSockets;
    }

    public void setDataSockets(List<Socket> dataSockets) {
        this.dataSockets.clear();
        this.dataSockets.addAll(dataSockets);
    }

    public BufferedReader getCommandConnReader() {
        return commandConnReader;
    }

    public BufferedWriter getCommandConnWriter() {
        return commandConnWriter;
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }

    public void setLoginSuccessful(boolean loginSuccessful) {
        this.loginSuccessful = loginSuccessful;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ServerSocket getPassiveModeServerSocket() {
        return passiveModeServerSocket;
    }

    public void setPassiveModeServerSocket(ServerSocket passiveModeServerSocket) {
        this.passiveModeServerSocket = passiveModeServerSocket;
    }

    public PassiveActive getPassiveActive() {
        return passiveActive;
    }

    public void setPassiveActive(PassiveActive passiveActive) {
        this.passiveActive = passiveActive;
    }

    public String getClientIPAddress() {
        return clientIPAddress;
    }

    public void setClientIPAddress(String clientIPAddress) {
        this.clientIPAddress = clientIPAddress;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }
}
