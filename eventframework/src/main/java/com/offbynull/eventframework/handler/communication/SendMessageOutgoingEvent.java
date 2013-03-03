package com.offbynull.eventframework.handler.communication;

import com.offbynull.eventframework.handler.DefaultTrackedOutgoingEvent;

public final class SendMessageOutgoingEvent
        extends DefaultTrackedOutgoingEvent {
    private Request request;
    private String address;
    private int port;

    public SendMessageOutgoingEvent(Request request, String address, int port,
            long trackedId) {
        super(trackedId);

        if (address == null) {
            throw new NullPointerException();
        }

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException();
        }
        
        this.request = request;
        this.address = address;
        this.port = port;
    }

    public Request getRequest() {
        return request;
    }

    public String getHost() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
