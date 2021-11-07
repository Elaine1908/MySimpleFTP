import server.core.MyFTPServerCore;

public class App {

    public static void main(String[] args) throws Exception {
        MyFTPServerCore myFTPServerCore = new MyFTPServerCore(3333, "D:\\");
        myFTPServerCore.start();
    }
}
