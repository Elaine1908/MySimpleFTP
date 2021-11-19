package server.core.response.concrete;

import server.core.response.AbstractResponse;

public class TransferSuccessResponse extends AbstractResponse {
    public TransferSuccessResponse() {
        super(226, "请求文件动作成功");
    }
}
