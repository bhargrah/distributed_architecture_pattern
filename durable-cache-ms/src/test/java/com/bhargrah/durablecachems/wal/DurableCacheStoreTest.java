package com.bhargrah.durablecachems.wal;

import com.bhargrah.durablecachems.TestUtils;
import com.bhargrah.durablecachems.wal.common.Config;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.Assert.assertEquals;

@SpringBootTest
public class DurableCacheStoreTest {

    @Test
    public void shouldRecoverKVStoreStateFromWAL() {
        //public static void main(String args[]) {
        File walDir = TestUtils.tempDir("distribute/patterns/wal");

        System.out.println("File Path : "+walDir.getAbsolutePath());
        DurableCacheStore kv = new DurableCacheStore(new Config(walDir.getAbsolutePath()));
        kv.put("title", "Microservices");
        //crash..
        //client got success;
        //client is sure that key1 is saved
        //fail.
        //success
        kv.put("author", "Martin");
        //fails.
        //retry
        kv.put("author", "Martin");
        //client is sure that key2 is saved
        kv.put("newTitle", "Distributed Systems");
        //client is sure that key3 is saved
        //}
        kv.put("architect", "Cloud Systems");
        //KV crashes.
        kv.close();

        //simulates process restart. A new instance is created at startup.
        DurableCacheStore recoveredKvStore = new DurableCacheStore(new Config(walDir.getAbsolutePath()));

        assertEquals(recoveredKvStore.get("title"), "Microservices");
        assertEquals(recoveredKvStore.get("author"), "Martin");
        assertEquals(recoveredKvStore.get("newTitle"), "Distributed Systems");
    }


}