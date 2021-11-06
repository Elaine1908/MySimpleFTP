package server.core;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


class MyFTPServerCoreTest {

    @Test
    public void testCreateFTPServerCore() {

        //测试正常创建
        try {
            MyFTPServerCore myFTPServerCore = new MyFTPServerCore(5563, "C:\\");
        } catch (IOException ignored) {
        }

        //测试rootPath不是目录
        assertThrows(IllegalArgumentException.class, () -> {
            MyFTPServerCore myFTPServerCore = new MyFTPServerCore(5563, "C:\\text.txt");
        });
    }

}