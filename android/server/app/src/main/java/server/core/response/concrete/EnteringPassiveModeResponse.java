package server.core.response.concrete;

import server.core.response.AbstractResponse;

import java.net.InetAddress;

public class EnteringPassiveModeResponse extends AbstractResponse {

    public EnteringPassiveModeResponse(String localAddress, int p1, int p2) {
        super(227, String.format("进入被动模式(%s,%d,%d)", localAddress.replace('.', ','), p1, p2));
    }
}
