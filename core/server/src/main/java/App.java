import com.alibaba.fastjson.JSON;
import server.core.MyFTPServerCore;
import server.core.command.concrete.RETRCommand;
import server.core.transmit.PartMeta;

public class App {

    public static void main(String[] args) throws Exception {
//        MyFTPServerCore myFTPServerCore = new MyFTPServerCore(3333, "D:\\");
//        myFTPServerCore.start();
        RETRCommand retrCommand = new RETRCommand("", "");
        System.out.println(retrCommand.getEachPartLength(1000,6));
    }
}
