package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.exception.CommandSyntaxWrongException;
import server.core.response.concrete.CommandOKResponse;
import server.core.response.concrete.NoImplementationResponse;
import server.core.response.concrete.NotLoginResponse;
import server.core.thread.HandleUserRequestThread;

import java.io.IOException;
import java.util.Objects;

public class TYPECommand extends AbstractCommand {
    public TYPECommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {

        //检查用户是否有登录，如果没有登录则给出没有登录的提醒
        if (!handleUserRequestThread.isLoginSuccessful()) {
            handleUserRequestThread.writeLine(new NotLoginResponse().toString());
            return;
        }

        //判断参数是否符合条件，如果不符合条件就直接给用户无法解析命令的提示
        if (!Objects.equals(commandArg, "A") && !Objects.equals(commandArg, "B")) {
            handleUserRequestThread.writeLine(new CommandSyntaxWrongException(commandType + " " + commandArg).toString());
            return;
        }

        //在thread中设置是ASCII还是binary传输
        if (Objects.equals(commandArg, "A")) {
            handleUserRequestThread.setAsciiBinary(HandleUserRequestThread.ASCIIBinary.ASCII);
        } else if (Objects.equals(commandArg, "B")) {
            handleUserRequestThread.setAsciiBinary(HandleUserRequestThread.ASCIIBinary.BINARY);
        }

        //把成功的信息写给用户
        handleUserRequestThread.writeLine(new CommandOKResponse().toString());
    }
}
