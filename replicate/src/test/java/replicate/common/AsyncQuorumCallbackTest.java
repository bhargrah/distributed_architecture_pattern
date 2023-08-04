package replicate.common;

import org.junit.Test;
import replicate.paxos.messages.PrepareResponse;

import static org.junit.Assert.assertTrue;

public class AsyncQuorumCallbackTest {
    @Test
    public void completesFutureIfQuorumIsNotMetAndTotalResponsesReceived() {
        AsyncQuorumCallback<PrepareResponse> callback = new AsyncQuorumCallback<>(3, p -> p.promised);
        callback.onResponse(new PrepareResponse(false), TestUtils.randomAddress());
        callback.onResponse(new PrepareResponse(false), TestUtils.randomAddress());
        callback.onError(new RuntimeException("Could not connect to " + TestUtils.randomAddress()));

        assertTrue(callback.quorumFuture.isCompletedExceptionally());
    }

}