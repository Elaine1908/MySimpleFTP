package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.concrete.NoImplementationResponse;
import server.core.thread.HandleUserRequestThread;

public class TYPECommand extends AbstractCommand {
    public TYPECommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) {
        handleUserRequestThread.writeLine(new NoImplementationResponse().toString());
    }
}
