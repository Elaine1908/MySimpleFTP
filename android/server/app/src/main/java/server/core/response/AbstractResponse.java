package server.core.response;

public abstract class AbstractResponse {
    protected int code;
    protected String message;

    public AbstractResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("%d %s", code, message);
    }
}
