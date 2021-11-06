import server.core.MyFTPServerCore;
import java.io.IOException;

public class App {

    public static void main(String[] args) throws InterruptedException, IOException {
        MyFTPServerCore myFTPServerCore = new MyFTPServerCore(3333, "D:\\");
        myFTPServerCore.start();

    }
}
