package server.core.command;

import server.core.thread.HandleUserRequestThread;

/**
 * 这是抽象用户传来的USER,PASS等命令的接口
 */
public abstract class AbstractCommand {

    protected final String commandType;
    protected final String commandArg;

    /**
     * 在handleUserRequestThread这个线程对应的控制连接和数据连接上执行这个命令
     *
     * @param handleUserRequestThread 线程
     */
    public abstract void execute(HandleUserRequestThread handleUserRequestThread);

    public AbstractCommand(String commandType, String commandArg) {
        this.commandType = commandType;
        this.commandArg = commandArg;
    }

    public String getCommandType() {
        return commandType;
    }

    public String getCommandArg() {
        return commandArg;
    }
}
