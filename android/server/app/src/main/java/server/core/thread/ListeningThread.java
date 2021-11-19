package server.core.thread;

import server.core.logger.FTPServerLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * 这是用来无限循环（监听）并accept用户的连接的线程
 */
public class ListeningThread extends Thread {

    private final ServerSocket serverSocket;//用来监听的ServerSocket
    private final String rootPath;//ftp服务器的根目录
    private final FTPServerLogger logger;


    /**
     * 传入一个ServerSocket(在这个套接字上监听并accept用户的连接请求)
     *
     * @param serverSocket 监听并accept用户连接请求的socket
     * @param logger       日志记录器
     */
    public ListeningThread(ServerSocket serverSocket, String rootPath, FTPServerLogger logger) {
        this.serverSocket = serverSocket;
        this.rootPath = rootPath;
        this.logger = logger;
    }

    @Override
    public void run() {
        while (true) {//无线循环，监听用户的连接

            Socket clientCommandSocket;
            try {

                //accept用户的连接请求，并将得到的socket作为这个用户的控制连接！
                clientCommandSocket = serverSocket.accept();

                //创建处理用户请求的线程
                HandleUserRequestThread handleUserRequestThread = new HandleUserRequestThread(clientCommandSocket, rootPath);

                //如果有日志记录器就加入
                if (logger != null) {
                    handleUserRequestThread.setLoggerEnabled(true);
                    handleUserRequestThread.setLogger(logger);
                }

                //线程开始运行
                handleUserRequestThread.start();
            } catch (IOException ignored) {
            }

        }
    }
}
