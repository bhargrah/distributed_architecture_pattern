package replicate.quorumconsensus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import replicate.common.*;
import replicate.net.InetAddressAndPort;
import replicate.quorumconsensus.messages.*;
import replicate.wal.DurableKVStore;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Quorum consensus algorithm as described by H. Attiya, A. Bar-Noy, and D. Dolev. "Sharing memory robustly in message-passing systems." Journal of the ACM, 42(1):124-142, Jan. 1995
 * http://web.mit.edu/6.033/2005/wwwdocs/quorum_note.html
 */
public class QuorumConsensus extends Replica {
    private static Logger logger = LogManager.getLogger(QuorumConsensus.class);

    public QuorumConsensus(String name, Config config, SystemClock clock, InetAddressAndPort clientConnectionAddress, InetAddressAndPort peerConnectionAddress, boolean doSyncReadRepair, List<InetAddressAndPort> peers) throws IOException {
        super(name, config, clock,clientConnectionAddress, peerConnectionAddress, peers);
        this.durableStore = new DurableKVStore(config);
    }

    @Override
    protected void registerHandlers() {
        handlesMessage(MessageId.GetVersion, this::handleGetVersionRequest, GetVersionRequest.class);
        handlesMessage(MessageId.GetVersionResponse, this::handleGetVersionResponse, GetVersionResponse.class);

        handlesMessage(MessageId.VersionedSetValueRequest, this::handlePeerSetValueRequest, VersionedSetValueRequest.class);
        handlesMessage(MessageId.SetValueResponse, this::handleSetValueResponse, SetValueResponse.class);

        handlesMessage(MessageId.VersionedGetValueRequest, this::handleGetValueRequest, GetValueRequest.class);
        handlesMessage(MessageId.GetValueResponse, this::handleGetValueResponse, GetValueResponse.class);

        handlesRequestAsync(MessageId.SetValueRequest, this::handleClientSetValueRequest, SetValueRequest.class);
        handlesRequestAsync(MessageId.GetValueRequest, this::handleClientGetValueRequest, GetValueRequest.class);
    }

    private void handleGetValueResponse(Message<GetValueResponse> getValueResponseMessage) {
        handleResponse(getValueResponseMessage);
    }

    private void handleSetValueResponse(Message<SetValueResponse> setValueResponseMessage) {
        handleResponse(setValueResponseMessage);
    }

    private void handleGetVersionResponse(Message<GetVersionResponse> message) {
        handleResponse(message);
    }

    private CompletableFuture<SetValueResponse> handleClientSetValueRequest(SetValueRequest clientSetValueRequest) {
        var getVersion = new GetVersionRequest(clientSetValueRequest.key);
        var versionCallback = new AsyncQuorumCallback<GetVersionResponse>(getNoOfReplicas());
        sendMessageToReplicas(versionCallback, MessageId.GetVersion, getVersion);
        var quorumFuture = versionCallback.getQuorumFuture();
        return quorumFuture.thenCompose((r) ->
                assignVersionAndSetValue(clientSetValueRequest, getExistingVersions(r)));
    }

    private static List<MonotonicId> getExistingVersions(Map<InetAddressAndPort, GetVersionResponse> versionResponses) {
        return versionResponses.values().stream().map(r -> r.getVersion()).collect(Collectors.toList());
    }

    private CompletableFuture<SetValueResponse> assignVersionAndSetValue(SetValueRequest clientSetValueRequest, List<MonotonicId> existingVersions) {
        var requestToReplicas = new VersionedSetValueRequest(clientSetValueRequest.key,
                clientSetValueRequest.value,
                getNextId(existingVersions)
        ); //assign timestamp to request.
        var quorumCallback = new AsyncQuorumCallback<SetValueResponse>(getNoOfReplicas());
        sendMessageToReplicas(quorumCallback, MessageId.VersionedSetValueRequest, requestToReplicas);
        var quorumFuture = quorumCallback.getQuorumFuture();
        return quorumFuture.thenApply(r -> {
            //TODO:Find how to handle multiple values;
            return r.values().stream().findFirst().get();
        });
    }


    private MonotonicId getNextId(List<MonotonicId> ids) {
        MonotonicId max = getMax(ids);
        if (max.isEmpty()) {
            return new MonotonicId(1, getServerId());
        }

        int requestId = max.requestId + 1;
        return new MonotonicId(requestId, getServerId());
    }

    private MonotonicId getMax(List<MonotonicId> ids) {
        return ids.stream().max(MonotonicId::compareTo).orElse(MonotonicId.empty());
    }

    private CompletableFuture<StoredValue> handleClientGetValueRequest(GetValueRequest request) {
        var asyncQuorumCallback = new AsyncQuorumCallback<GetValueResponse>(getNoOfReplicas());
        sendMessageToReplicas(asyncQuorumCallback, MessageId.VersionedGetValueRequest, request);
        return asyncQuorumCallback.getQuorumFuture()
                .thenCompose((nodesToValues)-> {
                    return new ReadRepairer(this, nodesToValues).readRepair();
                });
    }

    private void handleGetVersionRequest(Message<GetVersionRequest> message) {
        var getVersionRequest = message.messagePayload();
        StoredValue storedValue = get(getVersionRequest.getKey());
        MonotonicId version = (storedValue == null) ? MonotonicId.empty() : storedValue.getVersion();
        sendOneway(message.getFromAddress(), new GetVersionResponse(version), message.getCorrelationId());
    }

    private void handleGetValueRequest(Message<GetValueRequest> message) {
        var getValueRequest = message.messagePayload();
        StoredValue storedValue = get(getValueRequest.getKey());
        sendOneway(message.getFromAddress(), new GetValueResponse(storedValue), message.getCorrelationId());
    }

    //this will be invoked sequentially one after the other
    private void handlePeerSetValueRequest(Message<VersionedSetValueRequest> message) {
        //lock
        var setValueRequest = message.messagePayload();
        StoredValue storedValue = get(setValueRequest.key);
        if (setValueRequest.version.isAfter(storedValue.getVersion())) { //set only if setting with higher version timestamp.
            put(setValueRequest.key, new StoredValue(setValueRequest.key, setValueRequest.value, setValueRequest.version));
        }
        //do following in a separate thread of control.
        sendOneway(message.getFromAddress(), new SetValueResponse("Success"), message.getCorrelationId());
        //unlock
    }


    private final DurableKVStore durableStore;
    public void put(String key, StoredValue storedValue) {
        durableStore.put(key, JsonSerDes.toJson(storedValue));
    }

    public StoredValue get(String key) {
        String storedValue = durableStore.get(key);
        if (storedValue == null) {
            return StoredValue.EMPTY;
        }
        return JsonSerDes.fromJson(storedValue.getBytes(), StoredValue.class);
    }

    public MonotonicId getVersion(String key) {
        StoredValue storedValue = get(key);
        if (storedValue == null) {
            return MonotonicId.empty();
        }
        return storedValue.getVersion();
    }

}
