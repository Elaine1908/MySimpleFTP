package server.core.thread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 这是用来无限循环（监听）并accept用户的连接的线程
 */
public class ListeningThread extends Thread {

    private final ServerSocket serverSocket;//用来监听的ServerSocket

    /**
     * 传入一个ServerSocket(在这个套接字上监听并accept用户的连接请求)
     *
     * @param serverSocket 监听并accept用户连接请求的socket
     */
    public ListeningThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        while (true) {//无线循环，监听用户的连接

            Socket clientCommandSocket;
            try {

                //accept用户的连接请求，并将得到的socket作为这个用户的控制连接！
                clientCommandSocket = serverSocket.accept();

                //创建处理用户请求的线程
                Thread handleUserRequestThread = new HandleUserRequestThread(clientCommandSocket);

                //线程开始运行
                handleUserRequestThread.start();
            } catch (IOException ignored) {
            }


        }
    }
}
