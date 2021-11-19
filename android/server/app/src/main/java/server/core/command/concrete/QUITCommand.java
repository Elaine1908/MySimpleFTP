package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.concrete.ConnectionClosedResponse;
import server.core.thread.HandleUserRequestThread;

import java.io.IOException;

public class QUITCommand extends AbstractCommand {
    public QUITCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {
        //退出指令，写个221 服务器关闭控制连接给用户，关闭掉和这个用户所有的socket
        handleUserRequestThread.writeLine(new ConnectionClosedResponse().toString());
        handleUserRequestThread.closeAllConnections();
        throw new IOException();//抛出异常表示退出，连接已关闭
    }
}
