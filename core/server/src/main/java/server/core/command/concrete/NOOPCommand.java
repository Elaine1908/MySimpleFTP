package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.concrete.CommandOKResponse;
import server.core.response.concrete.NoImplementationResponse;
import server.core.thread.HandleUserRequestThread;

import java.io.IOException;

public class NOOPCommand extends AbstractCommand {

    public NOOPCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {
        //NOOP命令，啥都不做，直接返回给用户一个200 命令OK就行了
        handleUserRequestThread.writeLine(new CommandOKResponse().toString());
    }
}
