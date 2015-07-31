package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.SleepSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.info;
import static com.offbynull.peernetic.core.gateways.log.LogMessage.warn;
import com.offbynull.peernetic.core.shuttle.Address;
import static com.offbynull.peernetic.examples.unstructured.AddressConstants.ROUTER_HANDLER_RELATIVE_ADDRESS;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkFailedResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeepAliveRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkKeptAliveResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkSuccessResponse;
import com.offbynull.peernetic.visualizer.gateways.graph.AddEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.RemoveEdge;
import com.offbynull.peernetic.visualizer.gateways.graph.StyleEdge;
import java.time.Duration;
import org.apache.commons.lang3.Validate;

final class OutgoingLinkSubcoroutine implements Subcoroutine<Void> {

    private final Address subAddress;
    private final Address graphAddress;
    private final Address timerAddress;
    private final Address logAddress;
    private final State state;

    public OutgoingLinkSubcoroutine(
            Address subAddress,
            State state) {
        Validate.notNull(subAddress);
        Validate.notNull(state);
        this.subAddress = subAddress;
        this.graphAddress = state.getGraphAddress();
        this.timerAddress = state.getTimerAddress();
        this.logAddress = state.getLogAddress();
        this.state = state;
    }

    @Override
    public Address getAddress() {
        return subAddress;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();
        
        reconnect:
        while (true) {
            new SleepSubcoroutine.Builder()
                    .address(subAddress.appendSuffix(state.nextRandomId()))
                    .timerAddress(timerAddress)
                    .duration(Duration.ofSeconds(1L))
                    .build()
                    .run(cnt);
            
            if (!state.hasMoreCachedAddresses()) {
                ctx.addOutgoingMessage(subAddress, logAddress, warn("No further cached addresses are available"));
                continue;
            }
            
            String selfLinkId = state.getAddressTransformer().selfAddressToLinkId(ctx.getSelf());
            String outLinkId = state.getNextCachedLinkId();
            
            // make sure address we're connecting to isn't an already we're already connected to
            if (state.getLinks().contains(outLinkId)) {
                ctx.addOutgoingMessage(subAddress, logAddress, warn("Rejecting to link to {} (already linked), trying again", outLinkId));
                continue;
            }
            
            // make sure address we're conencting to isn't an already we're already CONNECTING TO (not connected to, but connecting to)
            if (state.getPendingOutgoingLinks().contains(outLinkId)) {
                ctx.addOutgoingMessage(subAddress, logAddress,
                        warn("Rejecting to link to {} (already attempting linking), trying again", outLinkId));
                continue;
            }

            ctx.addOutgoingMessage(subAddress, logAddress, info("Linking to {}", outLinkId));
            ctx.addOutgoingMessage(graphAddress, new AddEdge(selfLinkId, outLinkId));
            ctx.addOutgoingMessage(graphAddress, new StyleEdge(selfLinkId, outLinkId, "-fx-stroke: yellow"));
            boolean lineIsGreen = false;

            state.addPendingOutgoingLink(outLinkId);
            
            Address baseAddr = state.getAddressTransformer().linkIdToRemoteAddress(outLinkId);
            
            RequestSubcoroutine<Object> linkRequestSubcoroutine = new RequestSubcoroutine.Builder<>()
                    .address(subAddress.appendSuffix(state.nextRandomId()))
                    .request(new LinkRequest())
                    .timerAddress(timerAddress)
                    .destinationAddress(baseAddr.appendSuffix(ROUTER_HANDLER_RELATIVE_ADDRESS))
                    .throwExceptionIfNoResponse(false)
                    .addExpectedResponseType(LinkSuccessResponse.class)
                    .addExpectedResponseType(LinkFailedResponse.class)
                    .build();
            Object response = linkRequestSubcoroutine.run(cnt);

            if (response == null) {
                state.removePendingOutgoingLink(outLinkId);
                ctx.addOutgoingMessage(subAddress, logAddress, info("{} did not respond to link", outLinkId));
                ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLinkId, outLinkId));
                continue;
            } else if (response instanceof LinkFailedResponse) {
                state.removePendingOutgoingLink(outLinkId);
                ctx.addOutgoingMessage(subAddress, logAddress, info("{} responded with link failure", outLinkId));
                ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLinkId, outLinkId));
                continue;
            }
            
            LinkSuccessResponse successResponse = (LinkSuccessResponse) response;
            Address suffix = successResponse.getSuffix();
            state.addOutgoingLink(outLinkId, suffix);
            
            Address updateAddr = baseAddr.appendSuffix(suffix);

            connected:
            while (true) {
                ctx.addOutgoingMessage(subAddress, logAddress, info("Waiting to refresh link to {}", outLinkId));
                new SleepSubcoroutine.Builder()
                        .address(subAddress.appendSuffix(state.nextRandomId()))
                        .timerAddress(timerAddress)
                        .duration(Duration.ofSeconds(1L))
                        .build()
                        .run(cnt);
                
                ctx.addOutgoingMessage(subAddress, logAddress, info("Refreshing link to {}", outLinkId));
                
                RequestSubcoroutine<LinkKeptAliveResponse> keepAliveRequestSubcoroutine
                        = new RequestSubcoroutine.Builder<LinkKeptAliveResponse>()
                        .address(subAddress.appendSuffix(state.nextRandomId()))
                        .request(new LinkKeepAliveRequest())
                        .timerAddress(timerAddress)
                        .destinationAddress(updateAddr)
                        .throwExceptionIfNoResponse(false)
                        .addExpectedResponseType(LinkKeptAliveResponse.class)
                        .build();
                LinkKeptAliveResponse resp = keepAliveRequestSubcoroutine.run(cnt);
                
                if (resp == null) {
                    ctx.addOutgoingMessage(subAddress, logAddress, info("{} did not respond to link refresh", outLinkId));
                    ctx.addOutgoingMessage(graphAddress, new RemoveEdge(selfLinkId, outLinkId));
                    state.removeOutgoingLink(outLinkId);
                    continue reconnect;
                }
                
                ctx.addOutgoingMessage(subAddress, logAddress, info("{} responded to link refresh", outLinkId));
                    
                if (!lineIsGreen) {
                    ctx.addOutgoingMessage(graphAddress, new StyleEdge(selfLinkId, outLinkId, "-fx-stroke: green"));
                    lineIsGreen = true;
                }
            }
        }
    }

}
