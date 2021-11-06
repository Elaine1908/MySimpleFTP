import com.alibaba.fastjson.JSON;
import transmit.FileMeta;

import java.util.zip.GZIPInputStream;

public class App {

    public static void main(String[] args) throws InterruptedException {
        FileMeta fileMeta = new FileMeta(1024, "hello.java", FileMeta.TYPE.FILE);
        System.out.println(JSON.toJSONString(fileMeta));
        
    }
}
