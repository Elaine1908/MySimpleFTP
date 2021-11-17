import core.MyFTPClientCore;
import core.monitor.CommandLineDownloadUploadProgressMonitor;


public class App {
    public static void main(String[] args) throws Exception {
        MyFTPClientCore myFTPClientCore = null;
        myFTPClientCore = new MyFTPClientCore("127.0.0.1", 3334);
        boolean loginSuccess = myFTPClientCore.login("test", "test");
        myFTPClientCore.pasv();
        myFTPClientCore.type(MyFTPClientCore.ASCIIBinary.BINARY);
        myFTPClientCore.kali(MyFTPClientCore.KeepAlive.T);
        myFTPClientCore.addProgressMonitor(new CommandLineDownloadUploadProgressMonitor());
        myFTPClientCore.setDownloadDirectory("D:\\ftp_download");
        myFTPClientCore.retrieveSingleFile("one_gigabyte.data");

    }


}
