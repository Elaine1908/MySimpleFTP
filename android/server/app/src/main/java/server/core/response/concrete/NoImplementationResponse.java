package server.core.response.concrete;

import server.core.response.AbstractResponse;

public class NoImplementationResponse extends AbstractResponse {
    public NoImplementationResponse() {
        super(502, "这个命令没有实现");
    }
}
