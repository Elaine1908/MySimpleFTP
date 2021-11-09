package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.concrete.ArgumentWrongResponse;
import server.core.response.concrete.CommandOKResponse;
import server.core.response.concrete.NotLoginResponse;
import server.core.thread.HandleUserRequestThread;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 自己加的一个LFFR命令，用于列出服务器上一个文件夹中所有的文件，然后发送给客户端
 * LFFR = List Files in a FoldeR
 */
public class LFFRCommand extends AbstractCommand {

    private final List<String> filenameList = new ArrayList<>();

    public LFFRCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {
        //检查用户有没有登录
        if (!handleUserRequestThread.isLoginSuccessful()) {
            handleUserRequestThread.writeLine(new NotLoginResponse().toString());
            return;
        }

        //根目录对应的文件夹
        File rootPathFile = new File(handleUserRequestThread.getRootPath());
        //要列出所有文件的文件夹的File对象
        File folderFile = new File(handleUserRequestThread.getRootPath() + File.separator + commandArg);

        //判断是否存在，是文件夹，如果不是就返回给用户错误信息
        if (!folderFile.exists() || !folderFile.isDirectory()) {
            handleUserRequestThread.writeLine(new ArgumentWrongResponse("目标“文件夹”不存在或不是一个文件夹").toString());
            return;
        }

        //否则，则递归获取这个文件夹下的全部文件名
        filenameList.clear();
        backTrack(folderFile);

        //先给用户一个200 CommandOK
        handleUserRequestThread.writeLine(new CommandOKResponse().toString());

        //将每个文件的文件名去除掉根目录的前缀后，写给客户端
        for (String filename : filenameList
        ) {
            //去掉绝对目录文件名的根目录前缀
            String filenameWithoutPrefix = File.separator + new StringBuilder(filename).delete(0, rootPathFile.getAbsolutePath().length()).toString();

            //把反斜杠换成正斜杠
            filenameWithoutPrefix = filenameWithoutPrefix.replace("\\", "/");

            //写给客户
            handleUserRequestThread.writeLine(filenameWithoutPrefix);
        }

        //最后写一个<CRLF>，告诉用户结束了
        handleUserRequestThread.writeLine("");
    }

    /**
     * 递归遍历这个folder中的全部文件，并加入到类变量的filenameList中
     *
     * @param folder 要递归的根，即根文件夹！
     */
    private void backTrack(File folder) {
        File[] files = folder.listFiles();
        assert files != null;
        for (File f : files
        ) {
            if (f.isDirectory()) {
                backTrack(f);
            } else {
                filenameList.add(f.getAbsolutePath());
            }
        }
    }
}
