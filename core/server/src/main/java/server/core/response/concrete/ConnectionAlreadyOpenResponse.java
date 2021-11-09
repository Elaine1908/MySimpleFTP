package server.core.response.concrete;

import server.core.response.AbstractResponse;

public class ConnectionAlreadyOpenResponse extends AbstractResponse {

    public ConnectionAlreadyOpenResponse() {
        super(125, "数据连接已打开，传输开始");
    }
}
