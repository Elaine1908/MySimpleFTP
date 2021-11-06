package server.core;

import server.core.thread.ListeningThread;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * 简单的FTP服务器的Server的核心类
 */
public class MyFTPServerCore {


    private final int listenPort;//FTP服务器的监听接口

    private final String rootPath;//FTP服务器的根目录

    private final ServerSocket serverSocket; //用来监听的ServerSocket

    private final Thread listeningThread;//用来监听并

    /**
     * 初始化一个FTP服务器的核心，但是不开始运行它
     *
     * @param listenPort FTP服务器在哪个端口上监听
     * @param rootPath   FTP服务器的根目录
     */
    public MyFTPServerCore(int listenPort, String rootPath) throws IOException {
        this.listenPort = listenPort;
        this.rootPath = rootPath;

        //检查rootPath是否真的是一个目录，如果不是就抛出异常给用户
        if (!new File(rootPath).isDirectory()) {
            throw new IllegalArgumentException(String.format("传入的rootPath=%s作为ftp的根目录，必须是一个路径！", rootPath));
        }

        //在要监听的端口上打开一个ServerSocket
        serverSocket = new ServerSocket(listenPort);

        //主线程对象
        listeningThread = new ListeningThread(serverSocket);
    }


    /**
     * 开始运行这个ftp服务器
     */
    public void start() {
        //开始运行主线程，注意不要run
        listeningThread.start();
    }


}
