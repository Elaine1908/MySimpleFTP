package server.core.response.concrete;

import server.core.response.AbstractResponse;

public class BadCommandSequenceResponse extends AbstractResponse {

    public BadCommandSequenceResponse() {
        super(503, "命令顺序错误");
    }
}
