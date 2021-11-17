import core.MyFTPClientCore;
import core.monitor.CommandLineDownloadUploadProgressMonitor;


public class App {
    public static void main(String[] args) throws Exception {
        MyFTPClientCore myFTPClientCore = null;
        myFTPClientCore = new MyFTPClientCore("127.0.0.1", 3334);
        boolean loginSuccess = myFTPClientCore.login("test", "test");
        myFTPClientCore.pasv();
        myFTPClientCore.type(MyFTPClientCore.ASCIIBinary.BINARY);
        myFTPClientCore.kali(MyFTPClientCore.KeepAlive.F);
        myFTPClientCore.addProgressMonitor(new CommandLineDownloadUploadProgressMonitor());
        myFTPClientCore.setDownloadDirectory("D:\\ftp_download");
        long a = System.nanoTime();
        myFTPClientCore.retrieveFolder("small_files");
        long b = System.nanoTime();
        System.out.println((b - a) / 1000.0 / 1000.0);

    }


}
