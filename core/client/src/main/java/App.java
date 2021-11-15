import core.MyFTPClientCore;
import core.exception.ServerNotFoundException;

import java.io.*;
import java.util.List;

public class App {
    public static void main(String[] args) throws Exception {
        MyFTPClientCore myFTPClientCore = null;
        myFTPClientCore = new MyFTPClientCore("127.0.0.1", 3334);
        boolean loginSuccess = myFTPClientCore.login("test", "test");
        myFTPClientCore.kali(MyFTPClientCore.KeepAlive.T);
        List<String> filenames = myFTPClientCore.lffr("/pcs小程序/pcs_reg");

    }
}
