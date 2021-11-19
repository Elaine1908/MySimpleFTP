package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.concrete.ArgumentWrongResponse;
import server.core.response.concrete.CommandOKResponse;
import server.core.response.concrete.NotLoginResponse;
import server.core.thread.HandleUserRequestThread;

import java.io.IOException;

/**
 * KALI命令对应的Command
 */
public class KALICommand extends AbstractCommand {
    public KALICommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {
        //如果登录不成功就显示未登录
        if (!handleUserRequestThread.isLoginSuccessful()) {
            handleUserRequestThread.writeLine(new NotLoginResponse().toString());
            return;
        }

        //根据参数是T还是F分别判断
        if ("T".equals(commandArg)) {
            handleUserRequestThread.writeLine(new CommandOKResponse().toString());
            handleUserRequestThread.setKeepAlive("T");
            return;
        } else if ("F".equals(commandArg)) {
            handleUserRequestThread.writeLine(new CommandOKResponse().toString());
            handleUserRequestThread.setKeepAlive("F");
            return;
        }

        //运行到这里说明参数错误
        handleUserRequestThread.writeLine(new ArgumentWrongResponse().toString());
    }
}
