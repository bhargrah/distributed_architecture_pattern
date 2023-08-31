package com.bhargrah.durablecachems.service;

import com.bhargrah.durablecachems.entity.CacheEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class CacheService {

    HashMap<String,String> cache = new HashMap<>();

    public String lookupFromCache(String productKey) {
        return cache.get(productKey);
    }

    public void persistInCache(CacheEntry cacheEntry) {
        cache.put(cacheEntry.getProductKey(),cacheEntry.getProductName());
    }
}
