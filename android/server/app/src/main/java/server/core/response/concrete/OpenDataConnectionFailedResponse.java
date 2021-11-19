package server.core.response.concrete;

import server.core.response.AbstractResponse;

public class OpenDataConnectionFailedResponse extends AbstractResponse {

    public OpenDataConnectionFailedResponse() {
        super(425, "建立连接失败！");
    }
}
