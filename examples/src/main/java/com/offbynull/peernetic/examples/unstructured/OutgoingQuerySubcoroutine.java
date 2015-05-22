package com.offbynull.peernetic.examples.unstructured;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import com.offbynull.peernetic.core.actor.helpers.RequestSubcoroutine;
import com.offbynull.peernetic.core.actor.helpers.Subcoroutine;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.examples.unstructured.externalmessages.LinkFailedResponse;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryRequest;
import com.offbynull.peernetic.examples.unstructured.externalmessages.QueryResponse;
import org.apache.commons.lang3.Validate;

final class OutgoingQuerySubcoroutine implements Subcoroutine<Void> {

    private final Address sourceId;
    private final Address timerAddress;
    private final State state;

    public OutgoingQuerySubcoroutine(Address sourceId, Address timerAddress, State state) {
        Validate.notNull(sourceId);
        Validate.notNull(timerAddress);
        Validate.notNull(state);
        this.sourceId = sourceId;
        this.timerAddress = timerAddress;
        this.state = state;
    }

    @Override
    public Address getId() {
        return sourceId;
    }

    @Override
    public Void run(Continuation cnt) throws Exception {
        Context ctx = (Context) cnt.getContext();

        while (true) {
            Address address = state.getRandomCachedAddress();

            QueryRequest request = new QueryRequest();
            RequestSubcoroutine<QueryResponse> requestSubcoroutine = new RequestSubcoroutine.Builder<QueryResponse>()
                    .id(sourceId.appendSuffix("" + state.nextRandomId()))
                    .request(request)
                    .timerAddressPrefix(timerAddress)
                    .destinationAddress(address.appendSuffix("router", "handler"))
                    .throwExceptionIfNoResponse(false)
                    .addExpectedResponseType(LinkFailedResponse.class)
                    .build();
            QueryResponse response = requestSubcoroutine.run(cnt);
            
            state.addCachedAddresses(response.getLinks());
        }
    }

}
