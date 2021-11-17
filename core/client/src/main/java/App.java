import core.MyFTPClientCore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class App {
    public static void main(String[] args) throws Exception {
        MyFTPClientCore myFTPClientCore = null;
        myFTPClientCore = new MyFTPClientCore("127.0.0.1", 3334);
        boolean loginSuccess = myFTPClientCore.login("test", "test");
        myFTPClientCore.pasv();
        myFTPClientCore.type(MyFTPClientCore.ASCIIBinary.BINARY);
        myFTPClientCore.setDownloadDirectory("D:\\ftp_download");
        myFTPClientCore.kali(MyFTPClientCore.KeepAlive.T);
        myFTPClientCore.storeSingleFile("D:\\huajuanFTP", "咩咩咩");

    }
}
