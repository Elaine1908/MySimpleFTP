package server.core.exception;

/**
 * 命令的语法错误，FTP服务器无法解析此命令
 */
public class CommandSyntaxWrongException extends FTPServerException {

    /**
     * 命令的语法错误，FTP服务器无法解析此命令
     * @param wrongCommand 错误的命令
     */
    public CommandSyntaxWrongException(String wrongCommand) {
        super(500, String.format("无法识别命令%s，可能是命令的格式有误", wrongCommand));
    }
}
