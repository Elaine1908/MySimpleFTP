import server.core.MyFTPServerCore;
import server.core.logger.FTPServerLogger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class App {

    public static void main(String[] args) throws Exception {
        MyFTPServerCore myFTPServerCore = new MyFTPServerCore(7776, "D:\\ftp_server", new FTPServerLogger() {
            @Override
            public void info(String s) {
                System.out.println(s);
            }
        });
        myFTPServerCore.start();

    }
}
