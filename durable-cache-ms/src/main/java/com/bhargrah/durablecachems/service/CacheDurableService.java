package com.bhargrah.durablecachems.service;

import com.bhargrah.durablecachems.entity.CacheEntry;
import com.bhargrah.durablecachems.wal.DurableCacheStore;
import com.bhargrah.durablecachems.wal.common.Config;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class CacheDurableService {

    private static final String DIR = "/Users/rahulbhargava/Desktop/coding_pad/distributed_architecture_pattern/";
    private static final String PATH = "thumbstone/checkpoint/wal";
    private DurableCacheStore durableCacheStore;

    public CacheDurableService(){
        File walDir = new File(DIR,PATH);//+ random.nextInt(1000000));
        walDir.mkdirs();
        Config walConfig = new Config(walDir.getAbsolutePath());
        this.durableCacheStore = new DurableCacheStore(walConfig);
    }

    public String lookupFromDurableCache(String productKey) {
        return durableCacheStore.get(productKey);
    }

    public void persistInDurableCache(CacheEntry cacheEntry) {
        durableCacheStore.put(cacheEntry.getProductKey(),cacheEntry.getProductName());
    }
}
