package com.offbynull.peernetic.core;

import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.actors.retry.RetryProxyCoroutine;
import com.offbynull.peernetic.core.actors.retry.SimpleReceiveGuidelineGenerator;
import com.offbynull.peernetic.core.actors.retry.SimpleSendGuidelineGenerator;
import com.offbynull.peernetic.core.actors.retry.StartRetryProxy;
import com.offbynull.peernetic.core.actors.unreliable.SimpleLine;
import com.offbynull.peernetic.core.actors.unreliable.StartUnreliableProxy;
import com.offbynull.peernetic.core.actors.unreliable.UnreliableProxyCoroutine;
import com.offbynull.peernetic.core.gateway.Gateway;
import com.offbynull.peernetic.core.common.SimpleSerializer;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.gateways.udp.UdpGateway;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang3.Validate;

public class Test {

    public static void main(String[] args) throws InterruptedException {
//        basicTest();
//        basicTimer();
//        basicUdp();
//        basicUnreliable();
        basicRetry();
    }

    private static void basicTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                Validate.isTrue(i == (int) ctx.getIncomingMessage());
            }

            latch.countDown();
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        ActorThread echoerThread = ActorThread.create("echoer");
        ActorThread senderThread = ActorThread.create("sender");

        echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());
        senderThread.addOutgoingShuttle(echoerThread.getIncomingShuttle());
        echoerThread.addCoroutineActor("echoer", echoer);
        senderThread.addCoroutineActor("sender", sender, "echoer:echoer");

        latch.await();
    }

    private static void basicUnreliable() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage("hi", dstAddr, i);
                cnt.suspend();
                Validate.isTrue(i == (int) ctx.getIncomingMessage());
            }

            latch.countDown();
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        ActorThread echoerThread = ActorThread.create("echoer");
        ActorThread senderThread = ActorThread.create("sender");
        TimerGateway timerGateway = new TimerGateway("timer");

        echoerThread.addCoroutineActor("echoer", echoer);
        echoerThread.addCoroutineActor("proxy", new UnreliableProxyCoroutine(),
                new StartUnreliableProxy("timer", "echoer:echoer", new SimpleLine(12345L)));
        senderThread.addCoroutineActor("sender", sender, "sender:proxy:echoer:echoer");
        senderThread.addCoroutineActor("proxy", new UnreliableProxyCoroutine(),
                new StartUnreliableProxy("timer", "sender:sender", new SimpleLine(12345L)));

        echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());
        echoerThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        senderThread.addOutgoingShuttle(echoerThread.getIncomingShuttle());
        senderThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        timerGateway.addOutgoingShuttle(senderThread.getIncomingShuttle());
        timerGateway.addOutgoingShuttle(echoerThread.getIncomingShuttle());
        
        latch.await();
    }

    private static void basicUdp() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage(dstAddr, i);
                cnt.suspend();
                Validate.isTrue(i == (int) ctx.getIncomingMessage());
            }

            latch.countDown();
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        ActorThread echoerThread = ActorThread.create("echoer");
        Shuttle echoerInputShuttle = echoerThread.getIncomingShuttle();
        Gateway echoerUdpGateway = new UdpGateway(new InetSocketAddress(1000), "internaludp", echoerInputShuttle, "echoer:echoer", new SimpleSerializer());
        Shuttle echoerOutputShuttle = echoerUdpGateway.getIncomingShuttle();

        ActorThread senderThread = ActorThread.create("sender");
        Shuttle senderInputShuttle = senderThread.getIncomingShuttle();
        Gateway senderUdpGateway = new UdpGateway(new InetSocketAddress(2000), "internaludp", senderInputShuttle, "sender:sender", new SimpleSerializer());
        Shuttle senderOutputShuttle = senderUdpGateway.getIncomingShuttle();

        echoerThread.addOutgoingShuttle(echoerOutputShuttle);
        senderThread.addOutgoingShuttle(senderOutputShuttle);

        echoerThread.addCoroutineActor("echoer", echoer);
        senderThread.addCoroutineActor("sender", sender, "internaludp:7f000001.1000");

        latch.await();
    }

    private static void basicTimer() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine tester = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            String timerPrefix = ctx.getIncomingMessage();
            ctx.addOutgoingMessage(timerPrefix + ":2000", 0);
            cnt.suspend();

            latch.countDown();
        };

        TimerGateway timerGateway = new TimerGateway("timer");
        Shuttle timerInputShuttle = timerGateway.getIncomingShuttle();

        ActorThread testerThread = ActorThread.create("local");
        Shuttle testerInputShuttle = testerThread.getIncomingShuttle();

        testerThread.addOutgoingShuttle(timerInputShuttle);
        timerGateway.addOutgoingShuttle(testerInputShuttle);

        testerThread.addCoroutineActor("tester", tester, "timer");

        latch.await();
    }

    private static void basicRetry() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        Coroutine sender = (cnt) -> {
            Context ctx = (Context) cnt.getContext();
            String dstAddr = ctx.getIncomingMessage();

            for (int i = 0; i < 10; i++) {
                ctx.addOutgoingMessage("hi", dstAddr, i);
                cnt.suspend();
                System.out.println(i);
                Validate.isTrue(i == (int) ctx.getIncomingMessage());
            }

            latch.countDown();
        };

        Coroutine echoer = (cnt) -> {
            Context ctx = (Context) cnt.getContext();

            while (true) {
                String src = ctx.getSource();
                Object msg = ctx.getIncomingMessage();
                ctx.addOutgoingMessage(src, msg);
                cnt.suspend();
            }
        };

        ActorThread echoerThread = ActorThread.create("echoer");
        ActorThread senderThread = ActorThread.create("sender");
        TimerGateway timerGateway = new TimerGateway("timer");

        echoerThread.addCoroutineActor("echoer", echoer);
        echoerThread.addCoroutineActor("retry", new RetryProxyCoroutine(),
                new StartRetryProxy("timer", "echoer:echoer", x -> x.toString(), new SimpleSendGuidelineGenerator(), new SimpleReceiveGuidelineGenerator()));
        echoerThread.addCoroutineActor("unreliable", new UnreliableProxyCoroutine(),
                new StartUnreliableProxy("timer", "echoer:retry", new SimpleLine(12345L)));
        
        senderThread.addCoroutineActor("sender", sender, "sender:retry:echoer:unreliable");
        senderThread.addCoroutineActor("retry", new RetryProxyCoroutine(),
                new StartRetryProxy("timer", "sender:sender", x -> x.toString(), new SimpleSendGuidelineGenerator(), new SimpleReceiveGuidelineGenerator()));
        senderThread.addCoroutineActor("unreliable", new UnreliableProxyCoroutine(),
                new StartUnreliableProxy("timer", "sender:retry", new SimpleLine(12345L)));

        echoerThread.addOutgoingShuttle(senderThread.getIncomingShuttle());
        echoerThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        senderThread.addOutgoingShuttle(echoerThread.getIncomingShuttle());
        senderThread.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        timerGateway.addOutgoingShuttle(senderThread.getIncomingShuttle());
        timerGateway.addOutgoingShuttle(echoerThread.getIncomingShuttle());
        
        latch.await();
    }
}
