package core.exception;

public class FTPClientException extends Exception{
    public FTPClientException() {
    }

    public FTPClientException(String message) {
        super(message);
    }

    public FTPClientException(Throwable cause) {
        super(cause);
    }
}
