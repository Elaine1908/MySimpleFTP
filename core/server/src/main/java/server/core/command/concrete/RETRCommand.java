package server.core.command.concrete;

import com.alibaba.fastjson.JSON;
import server.core.command.AbstractCommand;
import server.core.response.concrete.NoImplementationResponse;
import server.core.response.concrete.TransferSuccessResponse;
import server.core.thread.HandleUserRequestThread;
import server.core.transmit.PartMeta;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class RETRCommand extends AbstractCommand {
    public RETRCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {
        String fileAbsolutePath = new File(handleUserRequestThread.getRootPath() + File.separator + commandArg).getAbsolutePath();

        //先尝试建立连接，建立连接时的一些错误提示已经在buildDataConnection函数里写给客户端了
        boolean connectionBuildSuccessful = handleUserRequestThread.buildDataConnection(
                fileAbsolutePath
        );

        if (!connectionBuildSuccessful) {//如果建立连接失败，建立连接时的一些错误提示已经在buildDataConnection函数里写给客户端了，这里直接结束这个函数就行
            return;
        }

        //准备开始传输数据，看是ASCII模式还是binary模式！
        if (handleUserRequestThread.getAsciiBinary() == HandleUserRequestThread.ASCIIBinary.ASCII) {//ASCII模式

            //ASCII模式下只用一个socket传输
            Socket dataSocket = handleUserRequestThread.getDataSockets().get(0);

            //用scanner读取文本文件，然后一行一行写给客户端
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));
            Scanner scanner = new Scanner(new FileInputStream(fileAbsolutePath));
            while (scanner.hasNext()) {
                bufferedWriter.write(scanner.nextLine());
                bufferedWriter.write("\r\n");
            }
            bufferedWriter.flush();

            //关闭数据连接
            handleUserRequestThread.getDataSockets().forEach(socket -> {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            });


        } else {//Binary模式

            //文件对象
            File file = new File(fileAbsolutePath);

            int partCnt = 2;

            //文件的总长度
            long totalLength = file.length();

            //每个字节的长度
            List<Long> eachPartLength = getEachPartLength(totalLength, partCnt);

            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));

            Thread[] threads = new Thread[partCnt];

            //开多线程，给在每个dataSocket上并行发送
            for (int i = 0; i < partCnt; i++) {

                int finalI = i;

                threads[i] = new Thread(() -> {
                    try {
                        byte[] content = inputStream.readNBytes(Math.toIntExact(eachPartLength.get(finalI)));
                        PartMeta partMeta = new PartMeta(content.length, commandArg, PartMeta.Compressed.NOT_COMPRESSED, finalI);

                        OutputStream outputStream = handleUserRequestThread.getDataSockets().get(finalI).getOutputStream();
                        outputStream.write(JSON.toJSONString(partMeta).getBytes(StandardCharsets.UTF_8));
                        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                        outputStream.write(content);

                        handleUserRequestThread.getDataSockets().get(finalI).close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                threads[i].start();
            }

            for (int i = 0; i < partCnt; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException ignored) {
                }
            }

            //关闭数据连接
            handleUserRequestThread.getDataSockets().forEach(socket -> {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            });

        }
        //最后给用户写一个传输成功的表示
        handleUserRequestThread.writeLine(new TransferSuccessResponse().toString());

    }

    public List<Long> getEachPartLength(long totalLength, int partCnt) {
        List<Long> eachPartLength = new ArrayList<>();
        for (int i = 0; i < partCnt; i++) {
            if (i < partCnt - 1) {
                eachPartLength.add(totalLength / partCnt);
            } else {
                eachPartLength.add(totalLength - totalLength / partCnt * (partCnt - 1));
            }
        }
        return eachPartLength;
    }
}
