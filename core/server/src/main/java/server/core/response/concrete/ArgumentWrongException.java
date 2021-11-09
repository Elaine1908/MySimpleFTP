package server.core.response.concrete;

import server.core.response.AbstractResponse;

public class ArgumentWrongException extends AbstractResponse {

    public ArgumentWrongException(String message) {
        super(501, message);
    }

    public ArgumentWrongException() {
        super(501, "参数语法错误");
    }
}
