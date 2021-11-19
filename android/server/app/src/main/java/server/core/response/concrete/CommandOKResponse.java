package server.core.response.concrete;

import server.core.response.AbstractResponse;

public class CommandOKResponse extends AbstractResponse {

    public CommandOKResponse() {
        super(200, "命令OK");
    }
}
