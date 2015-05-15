package com.offbynull.peernetic.examples.chord;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.coroutines.user.Coroutine;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour;
import com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.Controller;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetClosestPrecedingFingerResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetIdResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetPredecessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.GetSuccessorResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.NotifyRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.NotifyResponse;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableRequest;
import com.offbynull.peernetic.examples.chord.externalmessages.UpdateFingerTableResponse;
import com.offbynull.peernetic.examples.chord.internalmessages.Kill;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.chord.model.ExternalPointer;
import com.offbynull.peernetic.examples.chord.model.Pointer;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import com.offbynull.peernetic.examples.common.request.ExternalMessage;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleNode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChordClientCoroutine implements Coroutine {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChordClientCoroutine.class);

    @Override
    public void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        Start start = ctx.getIncomingMessage();
        Address timerPrefix = start.getTimerPrefix();
        Address graphAddress = start.getGraphAddress();
        NodeId selfId = start.getNodeId();
        Address bootstrapAddress = start.getBootstrapAddress();

        switchToStartedOnGraph(ctx, selfId, graphAddress);
        
        Set<Pointer> lastNotifiedPointers = new HashSet<>();
        try {
            State state = new State(timerPrefix, selfId, bootstrapAddress);

            
            // Join (or just initialize if no bootstrap node is set)
            JoinTask joinTask = new JoinTask(Address.of("join"), state);
            joinTask.run(cnt);

            switchToReadyOnGraph(ctx, selfId, graphAddress);
            lastNotifiedPointers = updateOutgoingLinksOnGraph(state, lastNotifiedPointers, ctx, selfId, graphAddress);

            
            // Create parent coroutine and add maintenance tasks to it
            Address mainSourceId = Address.of("main");
            
            SubcoroutineRouter router = new SubcoroutineRouter(mainSourceId, ctx);
            Controller controller = router.getController();
            
            controller.add(new UpdateOthersTask(mainSourceId.appendSuffix("updateothers"), state), AddBehaviour.ADD_PRIME); // notify our fingers
            controller.add(new FixFingerTableTask(mainSourceId.appendSuffix("fixfinger"), state), AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(new StabilizeTask(mainSourceId.appendSuffix("stabilize"), state), AddBehaviour.ADD_PRIME_NO_FINISH);
            controller.add(new CheckPredecessorTask(mainSourceId.appendSuffix("checkpred"), state), AddBehaviour.ADD_PRIME_NO_FINISH);
            

            while (true) {
                cnt.suspend();


                // Forward message to maintenance task. If the message wasn't for a maintenance task, try to handle it.
                boolean forwarded = router.forward();
                if (!forwarded) {
                    Object msg = ctx.getIncomingMessage();
                    Address fromAddress = ctx.getSource();

                    LOG.debug("{} {} - Processing {} from {} to {}", state.getSelfId(), "", msg.getClass(), fromAddress,
                            ctx.getDestination());

                    if (msg instanceof GetIdRequest) {
                        GetIdRequest extMsg = (GetIdRequest) msg;
                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new GetIdResponse(extMsg.getId(), state.getSelfId()));
                    } else if (msg instanceof GetClosestPrecedingFingerRequest) {
                        GetClosestPrecedingFingerRequest extMsg = (GetClosestPrecedingFingerRequest) msg;

                        Pointer pointer = state.getClosestPrecedingFinger(extMsg.getChordId(), extMsg.getIgnoreIds());
                        NodeId id = pointer.getId();
                        Address address = pointer instanceof ExternalPointer ? ((ExternalPointer) pointer).getAddress() : null;

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new GetClosestPrecedingFingerResponse(extMsg.getId(), id, address));
                    } else if (msg instanceof GetPredecessorRequest) {
                        GetPredecessorRequest extMsg = (GetPredecessorRequest) msg;

                        ExternalPointer pointer = state.getPredecessor();
                        NodeId id = pointer == null ? null : pointer.getId();
                        Address address = pointer == null ? null : pointer.getAddress();

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new GetPredecessorResponse(extMsg.getId(), id, address));
                    } else if (msg instanceof GetSuccessorRequest) {
                        GetSuccessorRequest extMsg = (GetSuccessorRequest) msg;

                        List<Pointer> successors = state.getSuccessors();

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new GetSuccessorResponse(extMsg.getId(),successors));
                    } else if (msg instanceof NotifyRequest) {
                        NotifyRequest extMsg = (NotifyRequest) msg;

                        NodeId requesterId = extMsg.getChordId();
                        Address requesterAddress = fromAddress.removeSuffix(2);

                        ExternalPointer newPredecessor = new ExternalPointer(requesterId, requesterAddress);
                        ExternalPointer existingPredecessor = state.getPredecessor();
                        if (existingPredecessor == null || requesterId.isWithin(existingPredecessor.getId(), true, state.getSelfId(), false)) {
                            state.setPredecessor(newPredecessor);
                        }

                        ExternalPointer pointer = state.getPredecessor();
                        NodeId id = pointer.getId();
                        Address address = pointer.getAddress();

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new NotifyResponse(extMsg.getId(), id, address));
                    } else if (msg instanceof UpdateFingerTableRequest) {
                        UpdateFingerTableRequest extMsg = (UpdateFingerTableRequest) msg;
                        NodeId id = extMsg.getChordId();
                        Address address = fromAddress.removeSuffix(2);
                        ExternalPointer newFinger = new ExternalPointer(id, address);

                        if (!state.isSelfId(id)) {
                            List<Pointer> oldFingers = state.getFingers();
                            boolean replaced = state.replaceFinger(newFinger);
                            List<Pointer> newFingers = state.getFingers();
                            LOG.debug("{} {} - Update finger with {}\nBefore: {}\nAfter: {}", state.getSelfId(), "",
                                    newFinger, oldFingers, newFingers);
//                            ExternalPointer pred = state.getPredecessor();
//                            if (replaced && pred != null) {
//                                ctx.addOutgoingMessage(
//                                        "ignore:ignore", // add 2 fake levels, because whoever gets this does a removeSuffix(2) (see above)
//                                        pred.getAddress(),
//                                        new UpdateFingerTableRequest(state.generateExternalMessageId(), id));
//                            }
                        }

                        addOutgoingExternalMessage(ctx,
                                fromAddress,
                                new UpdateFingerTableResponse(extMsg.getId()));
                    } else if (msg instanceof Kill) {
                        return;
                    }
                }
                
                
                lastNotifiedPointers = updateOutgoingLinksOnGraph(state, lastNotifiedPointers, ctx, selfId, graphAddress);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            for (Pointer ptr : lastNotifiedPointers) {
                disconnectOnGraph(ctx, selfId, ptr.getId(), graphAddress);
            }
            switchToDeadOnGraph(ctx, selfId, graphAddress);
        }
    }

    private Set<Pointer> updateOutgoingLinksOnGraph(State state, Set<Pointer> lastNotifiedPointers, Context ctx, NodeId selfId, Address graphAddress) {
        // Send link changes to graph
        Set<Pointer> newPointers = new HashSet<>(Arrays.<Pointer>asList(
                state.getFingers().stream().filter(x -> x instanceof ExternalPointer).toArray(x -> new Pointer[x])));
        if (state.getPredecessor() != null) {
            newPointers.add(state.getPredecessor());
        }
        
        Set<Pointer> addedPointers = new HashSet<>(newPointers);
        addedPointers.removeAll(lastNotifiedPointers);
        addedPointers.forEach(x -> connectOnGraph(ctx, selfId, x.getId(), graphAddress));
        
        Set<Pointer> removedPointers = new HashSet<>(lastNotifiedPointers);
        removedPointers.removeAll(newPointers);
        removedPointers.forEach(x -> disconnectOnGraph(ctx, selfId, x.getId(), graphAddress));
        
        lastNotifiedPointers = newPointers;
        return lastNotifiedPointers;
    }

    private void switchToStartedOnGraph(Context ctx, NodeId selfId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: yellow"));
    }

    private void switchToReadyOnGraph(Context ctx, NodeId selfId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: green"));
    }

    private void switchToDeadOnGraph(Context ctx, NodeId selfId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new StyleNode(selfId.toString(), "-fx-background-color: red"));
    }
    
    private void connectOnGraph(Context ctx, NodeId selfId, NodeId otherId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new AddEdge(selfId.toString(), otherId.toString()));
        ctx.addOutgoingMessage(graphAddress, new StyleEdge(selfId.toString(), otherId.toString(),
                "-fx-stroke: rgba(0, 0, 0, .5); -fx-stroke-width: 3;"));
    }

    private void disconnectOnGraph(Context ctx, NodeId selfId, NodeId otherId, Address graphAddress) {
        ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfId.toString(), otherId.toString()));
    }
    
    private void addOutgoingExternalMessage(Context ctx, Address destination, ExternalMessage message) {
        Validate.notNull(ctx);
        Validate.notNull(destination);
        Validate.notNull(message);
        
        ctx.addOutgoingMessage(
                Address.of("" + message.getId()),
                destination,
                message);
    }
    
}
