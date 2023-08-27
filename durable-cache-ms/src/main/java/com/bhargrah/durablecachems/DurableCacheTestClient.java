package com.bhargrah.durablecachems;

import com.bhargrah.durablecachems.wal.DurableCacheStore;
import com.bhargrah.durablecachems.wal.common.Config;
import java.io.File;

public class DurableCacheTestClient {

    public static final String DIR = "/Users/rahulbhargava/Desktop/coding_pad/distributed_architecture_pattern/replicate/out/";
    public static final String PATH = "distribute/patterns/wal";

  public static void main(String[] args) throws InterruptedException {

    File walDir = new File(DIR,PATH); walDir.mkdirs();
    Config walConfig = new Config(walDir.getAbsolutePath());

    DurableCacheStore kv = new DurableCacheStore(walConfig);

    kv.put("title", "Microservices");
    // crash..
    // client got success;
    // client is sure that key1 is saved
    // fail.
    // success
    kv.put("author", "Martin");
    // fails.
    // retry
    kv.put("author", "Martin");
    // client is sure that key2 is saved
    kv.put("newTitle", "Distributed Systems");
    // client is sure that key3 is saved
    // }
    kv.put("architect", "Cloud Systems");
    // KV crashes.
    kv.close();

    // simulates process restart. A new instance is created at startup.
    DurableCacheStore recoveredKvStore = new DurableCacheStore(new Config(walDir.getAbsolutePath()));
    recoveredKvStore.get("architect");

    System.out.println(recoveredKvStore.get("architect"));
    recoveredKvStore.close();

    //shutdown();
  }


  public static void shutdown() throws InterruptedException {
    Thread.sleep(5000);
    System.exit(0);
  }
}
