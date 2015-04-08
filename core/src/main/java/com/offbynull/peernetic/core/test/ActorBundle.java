package com.offbynull.peernetic.core.test;

import com.offbynull.peernetic.core.actor.Actor;
import com.offbynull.peernetic.core.actor.Context;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.lang3.Validate;

final class ActorBundle {
    private final String address;
    private final Actor actor;
    private final Duration timeOffset;
    private final Context context;
    
    private Instant earliestPossibleOnStepTime;

    public ActorBundle(String address, Actor actor, Duration timeOffset, Instant earliestPossibleOnStepTime) {
        Validate.notNull(address);
        Validate.notNull(actor);
        Validate.notNull(timeOffset);
        Validate.notNull(earliestPossibleOnStepTime);
        Validate.isTrue(!timeOffset.isNegative());
        this.address = address;
        this.actor = actor;
        this.timeOffset = timeOffset;
        this.context = new Context();
        this.earliestPossibleOnStepTime = earliestPossibleOnStepTime;
    }

    public Actor getActor() {
        return actor;
    }

    public Duration getTimeOffset() {
        return timeOffset;
    }

    public String getAddress() {
        return address;
    }

    public Context getContext() {
        return context;
    }

    public Instant getEarliestPossibleOnStepTime() {
        return earliestPossibleOnStepTime;
    }

    public void setEarliestPossibleOnStepTime(Instant earliestPossibleOnStepTime) {
        Validate.notNull(earliestPossibleOnStepTime);
        Validate.isTrue(!earliestPossibleOnStepTime.isBefore(this.earliestPossibleOnStepTime));
        this.earliestPossibleOnStepTime = earliestPossibleOnStepTime;
    }
}
