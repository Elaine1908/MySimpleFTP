package server.core.exception;

/**
 * FTP服务器中出现的异常的基类，其他具体的异常类可以继承它
 */
public class FTPServerException extends Exception {

    protected final int errorCode;

    public FTPServerException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public String toString() {
        return String.format("%d %s", errorCode, getMessage());
    }
}
