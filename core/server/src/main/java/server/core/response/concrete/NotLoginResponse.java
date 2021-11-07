package server.core.response.concrete;

import server.core.command.AbstractCommand;
import server.core.response.AbstractResponse;

public class NotLoginResponse extends AbstractResponse {

    public NotLoginResponse() {
        super(530, "未登录");
    }
}
