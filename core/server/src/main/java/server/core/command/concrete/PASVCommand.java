package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.concrete.NoImplementationResponse;
import server.core.thread.HandleUserRequestThread;

public class PASVCommand extends AbstractCommand {
    public PASVCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) {
        handleUserRequestThread.writeLine(new NoImplementationResponse().toString());
    }
}
