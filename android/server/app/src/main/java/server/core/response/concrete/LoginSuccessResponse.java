package server.core.response.concrete;

import server.core.response.AbstractResponse;

public class LoginSuccessResponse extends AbstractResponse {

    public LoginSuccessResponse() {
        super(230, "用户登录成功！");
    }
}
