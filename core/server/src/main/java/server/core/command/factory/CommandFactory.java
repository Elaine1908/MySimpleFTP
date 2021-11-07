package server.core.command.factory;

import server.core.command.AbstractCommand;
import server.core.exception.CommandSyntaxWrongException;

import java.lang.reflect.Constructor;

public class CommandFactory {

    /**
     * 根据控制连接上的一行（command），生产出对应的Command对象
     *
     * @param commandLine client在控制连接上输入的一行
     * @return 对应的Command对象
     */
    public static AbstractCommand parseCommand(String commandLine) throws CommandSyntaxWrongException {

        //获得第一个空格的index
        int firstSpaceIndex = commandLine.indexOf(" ");

        String commandType;
        String commandArg = null;

        //例如NOOP,PASV之类的命令是没有参数的，将有参数和无参数的命令分开处理
        if (firstSpaceIndex == -1) {
            commandType = commandLine.trim();
        } else {
            commandType = commandLine.substring(0, firstSpaceIndex);
            commandArg = commandLine.substring(firstSpaceIndex + 1).trim();
        }


        //获取对应的命令的具体类对象
        Class<?> clazz;
        try {
            clazz = Class.forName(String.format("server.core.command.concrete.%sCommand", commandType));

            //获得构造函数，第一个是命令类型，第二个是命令参数
            Constructor<?> constructor = clazz.getConstructor(String.class, String.class);

            //用构造函数创建对象并返回
            return (AbstractCommand) constructor.newInstance(commandType, commandArg);

        } catch (Throwable e) {
            throw new CommandSyntaxWrongException(commandLine);
        }
    }
}
