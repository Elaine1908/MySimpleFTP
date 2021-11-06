package server.core.thread;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * 此时FTP服务器已经accept了用户的连接请求，建立了控制连接，则新建此类的线程，专门用于处理用户的USER,PASS,STOR,RETR等命令
 */
public class HandleUserRequestThread extends Thread {

    private final Socket commandSocket;//和用户的控制连接，用户在控制连接上输入指令，然后服务端在控制连接上读取并解析用户的指令

    private final List<Socket> dataSockets;//和用户的数据连接（们），因为日后可能需要使用多个Socket，将一个大文件分开进行并发传输。

    /**
     * 在已经accept了用户的连接请求，获得了控制连接后，新建一个处理用户请求的线程！（但不启动它）
     *
     * @param commandSocket 与用户的控制连接
     */
    public HandleUserRequestThread(Socket commandSocket) {
        this.commandSocket = commandSocket;


        //创建一个数据连接的列表，现在列表里没有任何数据连接，等待用户有用PORT或PASV后，便有了数据连接
        dataSockets = new ArrayList<>();
    }


    @Override
    public void run() {
        super.run();
    }
}
