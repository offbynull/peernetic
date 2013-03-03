package com.offbynull.eventframework.handler.timer;

import com.offbynull.eventframework.handler.DefaultTrackedOutgoingEvent;

public final class NewTimerOutgoingEvent extends DefaultTrackedOutgoingEvent {
    private long duration;
    private Object data;

    public NewTimerOutgoingEvent(long trackedId, long duration) {
        this(trackedId, duration, null);
    }

    public NewTimerOutgoingEvent(long trackedId, long duration,
            Object data) {
        super(trackedId);
        
        if (duration < 0L) {
            throw new IllegalArgumentException();
        }
        this.duration = duration;
        this.data = data; // data can be null
    }

    public long getDuration() {
        return duration;
    }

    public Object getData() {
        return data;
    }
}
