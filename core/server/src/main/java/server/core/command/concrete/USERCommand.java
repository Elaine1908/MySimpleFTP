package server.core.command.concrete;

import server.core.command.AbstractCommand;
import server.core.response.concrete.LoginSuccessResponse;
import server.core.response.concrete.RequirePasswordResponse;
import server.core.thread.HandleUserRequestThread;

import java.util.Map;

public class USERCommand extends AbstractCommand {

    public USERCommand(String commandType, String commandArg) {
        super(commandType, commandArg);
    }

    @Override
    public void execute(HandleUserRequestThread handleUserRequestThread) {
        handleUserRequestThread.setLoginSuccessful(false);

        //参数名就是用户名！
        String username = commandArg;

        //设置登录用的用户名
        handleUserRequestThread.setUsername(username);

        handleUserRequestThread.setUsername(username);

        //获得用户名到密码的对象
        Map<String, String> usernameToPassword = handleUserRequestThread.usernameToPassword;

        //如果匿名用户，就直接登录成功
        if (usernameToPassword.containsKey(username) && usernameToPassword.get(username) == null) {

            handleUserRequestThread.writeLine(new LoginSuccessResponse().toString());
            handleUserRequestThread.setLoginSuccessful(true);

        } else {//否则则要求输入密码
            handleUserRequestThread.writeLine(new RequirePasswordResponse().toString());
        }
    }
}
