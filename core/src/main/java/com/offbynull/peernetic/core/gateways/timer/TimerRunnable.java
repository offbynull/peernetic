/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.core.gateways.timer;

import com.offbynull.peernetic.core.shuttle.AddressUtils;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.core.shuttle.Shuttle;
import com.offbynull.peernetic.core.shuttles.simple.Bus;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TimerRunnable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimerRunnable.class);
    
    private final Map<String, Shuttle> outgoingShuttles;
    private final PriorityQueue<PendingMessage> queue;
    private final Bus bus;

    public TimerRunnable(Bus bus) {
        outgoingShuttles = new HashMap<>();
        queue = new PriorityQueue<>(new PendingMessageSendTimeComparator());
        this.bus = bus;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Poll for new messages
                List<Object> incomingObjects;
                if (queue.isEmpty()) {
                    // Nothing in queue, so wait for ever
                    incomingObjects = bus.pull();
                } else {
                    // Something in queue, so wait until queue nearest item in queue needs to be sent
                    Instant currentTime = Instant.now();
                    Instant sendTime = queue.peek().getSendTime();
                    Duration duration = Duration.between(currentTime, sendTime);
                    if (duration.isNegative()) { // Sanity check. Depends on system clock if this happens.
                        duration = Duration.ZERO;
                    }
                    incomingObjects = bus.pull(duration.toMillis(), TimeUnit.MILLISECONDS);
                }
                
                Validate.notNull(incomingObjects);
                Validate.noNullElements(incomingObjects);
                Instant time = Instant.now();
                
                // Queue new messages
                for (Object incomingObj : incomingObjects) {
                    if (incomingObj instanceof Message) {
                        Message message = (Message) incomingObj;
                        String src = message.getSourceAddress();
                        String dst = message.getDestinationAddress();
                        Object payload = message.getMessage();

                        String delayStr = AddressUtils.getElement(dst, 1);
                        long delay = Long.parseLong(delayStr);
                        Validate.isTrue(delay >= 0L);
                        Instant sendTime = time.plus(delay, ChronoUnit.MILLIS);

                        queue.add(new PendingMessage(sendTime, dst, src, payload));
                    } else if (incomingObj instanceof AddShuttle) {
                        AddShuttle addShuttle = (AddShuttle) incomingObj;
                        Shuttle shuttle = addShuttle.getShuttle();
                        Shuttle existingShuttle = outgoingShuttles.putIfAbsent(shuttle.getPrefix(), shuttle);
                        Validate.validState(existingShuttle == null);
                    } else if (incomingObj instanceof RemoveShuttle) {
                        RemoveShuttle removeShuttle = (RemoveShuttle) incomingObj;
                        String prefix = removeShuttle.getPrefix();
                        Shuttle oldShuttle = outgoingShuttles.remove(prefix);
                        Validate.validState(oldShuttle != null);
                    }
                }
                
                // Group outgoing messages by prefix
                Map<String, List<Message>> outgoingMap = new HashMap<>();
                Iterator<PendingMessage> it = queue.iterator();
                while (it.hasNext()) {
                    // Read from queue up until we reach a message that's > time
                    PendingMessage pm = it.next();
                    if (pm.getSendTime().isAfter(time)) {
                        break;
                    }
                    it.remove();
                    
                    // Add to outgoingMap by prefix
                    String outDst = pm.getTo();
                    String outDstPrefix = AddressUtils.getElement(outDst, 0);

                    List<Message> batchedMessages = outgoingMap.get(outDstPrefix);
                    if (batchedMessages == null) {
                        batchedMessages = new LinkedList<>();
                        outgoingMap.put(outDstPrefix, batchedMessages);
                    }

                    Message message = new Message(pm.getFrom(), pm.getTo(), pm.getMessage());
                    batchedMessages.add(message);
                }
                

                // Send outgoing messaged by prefix
                for (Entry<String, List<Message>> entry : outgoingMap.entrySet()) {
                    Shuttle shuttle = outgoingShuttles.get(entry.getKey());
                    
                    if (shuttle == null) {
                        LOGGER.warn("No shuttle for {}", entry.getKey());
                        continue;
                    }
                    
                    shuttle.send(entry.getValue());
                }
            }
        } catch (InterruptedException ie) {
            bus.close(); // just in case
        }
    }

    private static final class PendingMessage {

        private final Instant sendTime;
        private final String from;
        private final String to;
        private final Object message;

        public PendingMessage(Instant sendTime, String from, String to, Object message) {
            Validate.notNull(from);
            Validate.notNull(to);
            Validate.notNull(message);
            this.sendTime = sendTime;
            this.from = from;
            this.to = to;
            this.message = message;
        }

        public Instant getSendTime() {
            return sendTime;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public Object getMessage() {
            return message;
        }

    }
    
    private static final class PendingMessageSendTimeComparator implements Comparator<PendingMessage>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(PendingMessage o1, PendingMessage o2) {
            return o1.getSendTime().compareTo(o2.getSendTime());
        }
        
    }

}
