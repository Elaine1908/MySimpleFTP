import com.alibaba.fastjson.JSON;
import server.core.MyFTPServerCore;
import server.core.transmit.PartMeta;

public class App {

    public static void main(String[] args) throws Exception {
//        MyFTPServerCore myFTPServerCore = new MyFTPServerCore(3333, "D:\\");
//        myFTPServerCore.start();
        System.out.println(JSON.toJSONString(new PartMeta(1024, "/hello.java", PartMeta.Compressed.NOT_COMPRESSED, 0)));
    }
}
