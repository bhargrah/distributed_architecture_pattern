package replicate.common;

import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import replicate.net.InetAddressAndPort;
import replicate.net.requestwaitinglist.RequestCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BlockingQuorumCallback<T> implements RequestCallback<T> {
    Logger logger = LogManager.getLogger(BlockingQuorumCallback.class);
    protected final int quorum;
    private final int totalResponses;
    List<Exception> exceptions = new ArrayList<>();
    protected Map<InetAddressAndPort, T> responses = new ConcurrentHashMap<>();
    CountDownLatch latch;

    public BlockingQuorumCallback(int totalResponses) {
        this.totalResponses = totalResponses;
        this.quorum = totalResponses / 2 + 1;
        this.latch = new CountDownLatch(quorum);
    }

    @Override
    public void onResponse(T r, InetAddressAndPort address) {
        logger.info("Received " + r + " from " + address);
        responses.put(address, r);
        latch.countDown();
    }

    @Override
    public void onError(Exception e) {
        exceptions.add(e);
    }

    public Map<InetAddressAndPort, T> blockAndGetQuorumResponses() {
        Uninterruptibles.awaitUninterruptibly(latch, 5000, TimeUnit.MILLISECONDS);
        return responses;
    }
}
