package com.offbynull.peernetic.core.actors.reliableproxy;

import java.time.Duration;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.collections4.set.UnmodifiableSortedSet;
import org.apache.commons.lang3.Validate;

public final class SendGuideline {
    private final String messageId;
    private final UnmodifiableSortedSet<Duration> sendDurations;
    private final Duration ackWaitDuration;

    public SendGuideline(String messageId, Duration ackWaitDuration, Duration ... sendDurations) {
        Validate.notNull(messageId);
        Validate.notNull(ackWaitDuration);
        Validate.isTrue(!ackWaitDuration.isNegative());
        Validate.notNull(sendDurations);
        
        SortedSet<Duration> retryDurationSet = new TreeSet<>();
        for (Duration retryDuration : sendDurations) {
            Validate.notNull(retryDuration);
            Validate.isTrue(!retryDuration.isNegative());
            
            retryDurationSet.add(retryDuration);
            Validate.isTrue(ackWaitDuration.compareTo(retryDuration) >= 0);
        }
        
        this.messageId = messageId;
        this.sendDurations = (UnmodifiableSortedSet<Duration>) UnmodifiableSortedSet.unmodifiableSortedSet(retryDurationSet);
        this.ackWaitDuration = ackWaitDuration;
    }

    public String getMessageId() {
        return messageId;
    }

    public UnmodifiableSortedSet<Duration> getSendDurations() {
        return sendDurations;
    }

    public Duration getAckWaitDuration() {
        return ackWaitDuration;
    }

    
}