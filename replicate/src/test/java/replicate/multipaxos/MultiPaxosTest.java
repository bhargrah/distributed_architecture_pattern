package replicate.multipaxos;

import org.junit.Before;
import org.junit.Test;
import replicate.common.ClusterTest;
import replicate.common.NetworkClient;
import replicate.common.TestUtils;
import replicate.paxos.messages.GetValueResponse;
import replicate.quorum.messages.GetValueRequest;
import replicate.twophaseexecution.messages.ExecuteCommandRequest;
import replicate.twophaseexecution.messages.ExecuteCommandResponse;
import replicate.wal.SetValueCommand;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class MultiPaxosTest extends ClusterTest<MultiPaxos> {
    @Before
    public void setUp() throws IOException {
        super.nodes = TestUtils.startCluster(nodeNames("athens", "byzantium", "cyrene"),
                (name, config, clock, clientConnectionAddress, peerConnectionAddress, peers) -> new MultiPaxos(name, clock, config, clientConnectionAddress, peerConnectionAddress, peers));

    }

    private static List<String> nodeNames(String... names) {
        return Arrays.asList(names);
    }

    @Test
    public void setsSingleValue() throws Exception {
        var athens = nodes.get("athens");
        athens.leaderElection();
        TestUtils.waitUntilTrue(() -> {
            return athens.isLeader();
        }, "Waiting for leader election", Duration.ofSeconds(2));

        var networkClient = new NetworkClient();
        byte[] command = new SetValueCommand("title", "Microservices").serialize();
        var setValueResponse = networkClient.sendAndReceive(new ExecuteCommandRequest(command), nodes.get("athens").getClientConnectionAddress(), ExecuteCommandResponse.class).getResult();
        assertEquals(Optional.of("Microservices"), setValueResponse.getResponse());
    }

    @Test
    public void singleValueNullPaxosGetTest() throws Exception {
        var athens = nodes.get("athens");

        athens.leaderElection();
        TestUtils.waitUntilTrue(() -> {
            return athens.isLeader();
        }, "Waiting for leader election", Duration.ofSeconds(2));

        var networkClient = new NetworkClient();
        var getValueResponse = networkClient.sendAndReceive(new GetValueRequest("title"), nodes.get("athens").getClientConnectionAddress(), GetValueResponse.class).getResult();
        assertEquals(Optional.empty(), getValueResponse.value);
    }

    @Test
    public void singleValuePaxosGetTest() throws Exception {
        var athens = nodes.get("athens");

        athens.leaderElection();
        TestUtils.waitUntilTrue(() -> {
            return athens.isLeader();
        }, "Waiting for leader election", Duration.ofSeconds(2));

        var networkClient = new NetworkClient();
        byte[] command = new SetValueCommand("title", "Microservices").serialize();
        var setValueResponse = networkClient.sendAndReceive(new ExecuteCommandRequest(command), nodes.get("athens").getClientConnectionAddress(), ExecuteCommandResponse.class);

        var getValueResponse = networkClient.sendAndReceive(new GetValueRequest("title"), nodes.get("athens").getClientConnectionAddress(), GetValueResponse.class).getResult();
        assertEquals(Optional.of("Microservices"), getValueResponse.value);
    }


    @Test //FIXME: flacky test
    public void leaderElectionCompletesIncompletePaxosRuns() throws Exception {
        MultiPaxos athens = nodes.get("athens");
        MultiPaxos byzantium = nodes.get("byzantium");
        MultiPaxos cyrene = nodes.get("cyrene");

        athens.leaderElection();

        TestUtils.waitUntilTrue(() -> {
            return athens.isLeader();
        }, "Waiting for leader election", Duration.ofSeconds(2));

        var networkClient = new NetworkClient();
        byte[] command = new SetValueCommand("title", "Microservices").serialize();
        var setValueResponse = networkClient.sendAndReceive(new ExecuteCommandRequest(command), athens.getClientConnectionAddress(), ExecuteCommandResponse.class);

        athens.dropMessagesTo(byzantium); //propose messages fail
        athens.dropMessagesTo(cyrene); //propose messages fail


        command = new SetValueCommand("author", "Martin").serialize();
        var response1 = networkClient.sendAndReceive(new ExecuteCommandRequest(command), athens.getClientConnectionAddress(), ExecuteCommandResponse.class);
        assertTrue("Expected to fail because athens will be unable to reach quorum", response1.isError());

        assertEquals(2, athens.paxosLog.size()); //uncommitted second entry
        assertEquals(1, byzantium.paxosLog.size()); //only first entry.
        assertEquals(1, cyrene.paxosLog.size()); //only first entry.

        assertTrue(athens.paxosLog.get(0).committedValue().isPresent());
        assertTrue(byzantium.paxosLog.get(0).committedValue().isPresent());
        assertTrue(cyrene.paxosLog.get(0).committedValue().isPresent());

        assertFalse(athens.paxosLog.get(1).committedValue().isPresent());

        athens.reconnectTo(byzantium);
        athens.reconnectTo(cyrene);

        assertNull(athens.getValue("author"));

        //election which is equivalent to prepare phase of basic paxos, checks
        //and completes pending log entries from majority quorum of the servers.
        byzantium.leaderElection();
        TestUtils.waitUntilTrue(() -> {
            return byzantium.isLeader();
        }, "Waiting for leader election", Duration.ofSeconds(2));

        assertEquals(2, athens.paxosLog.size());
        assertEquals(2, byzantium.paxosLog.size());
        assertEquals(2, cyrene.paxosLog.size());

        assertEquals("Martin", athens.getValue("author"));
        assertEquals("Martin", byzantium.getValue("author"));
        assertEquals("Martin", cyrene.getValue("author"));
    }
}