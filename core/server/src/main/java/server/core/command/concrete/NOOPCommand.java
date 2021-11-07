package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.concrete.NoImplementationResponse;
import server.core.thread.HandleUserRequestThread;

import java.io.IOException;

public class NOOPCommand extends AbstractCommand {

    public NOOPCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {
        handleUserRequestThread.writeLine(new NoImplementationResponse().toString());
    }
}
