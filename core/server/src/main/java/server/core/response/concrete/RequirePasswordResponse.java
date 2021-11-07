package server.core.response.concrete;

import server.core.response.AbstractResponse;

public class RequirePasswordResponse extends AbstractResponse {
    public RequirePasswordResponse() {
        super(331, "请输入密码");
    }
}
