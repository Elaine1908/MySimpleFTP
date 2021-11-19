package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.concrete.LoginSuccessResponse;
import server.core.response.concrete.NotLoginResponse;
import server.core.thread.HandleUserRequestThread;

import java.io.IOException;
import java.util.Map;

public class PASSCommand extends AbstractCommand {

    public PASSCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) throws IOException {
        String password = commandArg;
        String username = handleUserRequestThread.getUsername();
        Map<String, String> usernameToPassword = handleUserRequestThread.usernameToPassword;

        //如果username为null或者密码不匹配，则登录失败
        if (username == null || !usernameToPassword.containsKey(username) || !usernameToPassword.get(username).equals(password)) {
            handleUserRequestThread.writeLine(new NotLoginResponse().toString());
        } else {
            //否则就登录成功
            handleUserRequestThread.writeLine(new LoginSuccessResponse().toString());
            handleUserRequestThread.setLoginSuccessful(true);
        }
    }
}
