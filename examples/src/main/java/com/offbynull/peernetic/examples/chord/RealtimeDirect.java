package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.actor.ActorRunner;
import com.offbynull.peernetic.examples.common.ConsoleStage;
import static com.offbynull.peernetic.core.actor.helpers.IdGenerator.MIN_SEED_SIZE;
import com.offbynull.peernetic.core.gateways.log.LogGateway;
import com.offbynull.peernetic.core.gateways.timer.TimerGateway;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.examples.chord.internalmessages.Kill;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.NodeId;
import com.offbynull.peernetic.core.actor.helpers.SimpleAddressTransformer;
import com.offbynull.peernetic.visualizer.gateways.graph.GraphGateway;
import java.util.Collections;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.commons.lang3.Validate;

public final class RealtimeDirect {

    private static final String BASE_ACTOR_ADDRESS_STRING = "actor";
    private static final String BASE_GRAPH_ADDRESS_STRING = "graph";
    private static final String BASE_TIMER_ADDRESS_STRING = "timer";
    private static final String BASE_LOG_ADDRESS_STRING = "log";

    private static final Address BASE_ACTOR_ADDRESS = Address.of(BASE_ACTOR_ADDRESS_STRING);
    private static final Address BASE_GRAPH_ADDRESS = Address.of(BASE_GRAPH_ADDRESS_STRING);
    private static final Address BASE_TIMER_ADDRESS = Address.of(BASE_TIMER_ADDRESS_STRING);
    private static final Address BASE_LOG_ADDRESS = Address.of(BASE_LOG_ADDRESS_STRING);

    public static void main(String[] args) throws Exception {
        GraphGateway.startApplication();

        GraphGateway graphGateway = new GraphGateway(BASE_GRAPH_ADDRESS_STRING);
        LogGateway logGateway = new LogGateway(BASE_LOG_ADDRESS_STRING);
        TimerGateway timerGateway = new TimerGateway(BASE_TIMER_ADDRESS_STRING);
        ActorRunner actorRunner = new ActorRunner(BASE_ACTOR_ADDRESS_STRING);

        timerGateway.addOutgoingShuttle(actorRunner.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(logGateway.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(timerGateway.getIncomingShuttle());
        actorRunner.addOutgoingShuttle(graphGateway.getIncomingShuttle());

        graphGateway.addStage(() -> new ConsoleStage());
        ConsoleStage consoleStage = ConsoleStage.getInstance();

        ArrayBlockingQueue<Integer> sizeContainer = new ArrayBlockingQueue<>(1);
        consoleStage.outputLine("Enter size of Chord ring (must be power of 2)");
        consoleStage.setCommandProcessor((input) -> {
            sizeContainer.add(Integer.parseInt(input));
            return "Creating " + input + " nodes";
        });
        int size = sizeContainer.take();

        Validate.isTrue(size > 0, "Bad size");
        Validate.isTrue(Integer.bitCount(size) == 1, "Not power of 2");

        int bits = Integer.numberOfTrailingZeros(size); // For example: 16 is 1000b, 1000b has 3 trailing zeros, so number of bits for
                                                        // ring-space is 3. Nodes will start from 0 to 15.

        graphGateway.setHandlers(new CustomGraphNodeAddHandler(size), new CustomGraphNodeRemoveHandler());

        consoleStage.outputLine("Node colors");
        consoleStage.outputLine("-----------");
        consoleStage.outputLine("Red = Not active");
        consoleStage.outputLine("Yellow = Initializing");
        consoleStage.outputLine("Green = Active");
        consoleStage.outputLine("");
        consoleStage.outputLine("Line colors");
        consoleStage.outputLine("-----------");
        consoleStage.outputLine("Red = Finger");
        consoleStage.outputLine("Green = Successor");
        consoleStage.outputLine("Blue = Predecessor");
        consoleStage.outputLine("Yellow (Red+Green) = Finger+Successor");
        consoleStage.outputLine("Purple (Red+Blue) = Finger+Predecessor");
        consoleStage.outputLine("White (Red+Green+Blue) = Finger+Successor+Predecessor");
        consoleStage.outputLine("");
        consoleStage.outputLine("Available commands");
        consoleStage.outputLine("------------------");
        consoleStage.outputLine("Bootstrap node: boot <id>");
        consoleStage.outputLine("Connect nodes: start <start_id> <end_id> <connect_id>");
        consoleStage.outputLine("Stop nodes: stop <start_id> <end_id>");
        consoleStage.outputLine("To shutdown: exit");
        consoleStage.outputLine("");

        consoleStage.setCommandProcessor((input) -> {
            Scanner scanner = new Scanner(input);
            scanner.useDelimiter("\\s+");

            switch (scanner.next().toLowerCase()) {
                case "boot": {
                    int id = scanner.nextInt();
                    addNode(id, null, bits, actorRunner);
                    return "Executed command: " + input;
                }
                case "start": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();
                    int connectId = scanner.nextInt();

                    Validate.isTrue(startId <= endId);

                    for (int id = startId; id <= endId; id++) {
                        addNode(id, connectId, bits, actorRunner);
                    }
                    return "Executed command: " + input;
                }
                case "stop": {
                    int startId = scanner.nextInt();
                    int endId = scanner.nextInt();

                    Validate.isTrue(startId <= endId);

                    for (int id = startId; id <= endId; id++) {
                        removeNode(id, actorRunner);
                    }
                    return "Executed command: " + input;
                }
                case "exit": {
                    GraphGateway.exitApplication();
                    return "Executed command: " + input;
                }
                default: {
                    return "Unknown command: " + input;
                }
            }
        });

        GraphGateway.awaitShutdown();
    }
    
    private static void addNode(int id, Integer connId, int bits, ActorRunner actorRunner) {
        String idStr = Integer.toString(id);
        String connIdStr = connId == null ? null : connId.toString();
        
        byte[] seed = new byte[MIN_SEED_SIZE];
        seed[0] = (byte) id;

        actorRunner.addCoroutineActor(
                idStr,
                new ChordClientCoroutine(),
                new Start(
                        new SimpleAddressTransformer(BASE_ACTOR_ADDRESS, idStr),
                        connIdStr,
                        new NodeId(id, bits),
                        seed,
                        BASE_TIMER_ADDRESS,
                        BASE_GRAPH_ADDRESS,
                        BASE_LOG_ADDRESS
                )
        );
    }

    private static void removeNode(int id, ActorRunner actorRunner) {
        String idStr = Integer.toString(id);

        actorRunner.getIncomingShuttle().send(
                Collections.singleton(
                        new Message(
                                BASE_ACTOR_ADDRESS.appendSuffix(idStr),
                                BASE_ACTOR_ADDRESS.appendSuffix(idStr),
                                new Kill()
                        )
                )
        );
    }
}
