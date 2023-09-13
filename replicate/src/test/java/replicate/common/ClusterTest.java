package replicate.common;

import org.junit.After;
import org.junit.Before;
import replicate.quorum.QuorumKVStore;
import replicate.quorum.messages.GetValueResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class ClusterTest<T extends Replica> {

    protected Map<String, T> nodes = new HashMap<String, T>();
    @Before
    public void setUp() throws IOException {
    }
    @After
    public void tearDown() {
        nodes.values().stream().forEach(n -> n.shutdown());
    }

    protected void assertTitleValues(QuorumKVStore[] nodes, String[] expectedTitles) {
        assertEquals(nodes.length, expectedTitles.length);

        for (int i = 0; i < nodes.length; i++) {
            assertEquals(expectedTitles[i], nodes[i].get("title").getValue());
        }
    }

    protected void assertTitleEquals(QuorumKVStore quorumKVStore, String microservices) {
        assertEquals(microservices, quorumKVStore.get("title").getValue());
    }

    protected static void assertResponseSuccess(NetworkClient.Response<?> response) {
        assertTrue(response.isSuccess());
    }

    protected static void assertResponseFailure(NetworkClient.Response<?> response) {
        assertTrue(response.isError());
    }

    protected static String valueFrom(NetworkClient.Response<GetValueResponse> titleResponse) {
        return titleResponse.getResult().getValue().getValue();
    }
}
