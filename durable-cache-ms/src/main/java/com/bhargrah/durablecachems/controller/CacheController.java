package com.bhargrah.durablecachems.controller;

import com.bhargrah.durablecachems.entity.CacheEntry;
import com.bhargrah.durablecachems.service.CacheDurableService;
import com.bhargrah.durablecachems.service.CacheService;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api")
public class CacheController {

    @Autowired
    private final CacheDurableService cacheDurableService;
    @Autowired
    private final CacheService cacheService;

    public CacheController (CacheDurableService cacheDurableService ,CacheService cacheService ){
        this.cacheDurableService=cacheDurableService;
        this.cacheService=cacheService;
    }

    @GetMapping("lookup/cache/durable/{productKey}")
    @ResponseStatus(HttpStatus.OK)
    public String lookupFromDurableCache(@PathVariable("productKey") String productKey){
        String result = cacheDurableService.lookupFromDurableCache(productKey);
        return Strings.isNullOrEmpty(result) ? "No product found" : result;
    }

    @PostMapping("persist/durable")
    @ResponseStatus(HttpStatus.CREATED)
    public String persistInDurableCache(@RequestBody CacheEntry cacheEntry){
        cacheDurableService.persistInDurableCache(cacheEntry);
        return "Product details added successfully in durable cache";
    }

    @GetMapping("lookup/cache/{productKey}")
    @ResponseStatus(HttpStatus.OK)
    public String lookupFromCache(@PathVariable("productKey") String productKey){
        String result = cacheService.lookupFromCache(productKey);
        return Strings.isNullOrEmpty(result) ? "No product found" : result;
    }

    @PostMapping("persist")
    @ResponseStatus(HttpStatus.CREATED)
    public String persistInCache(@RequestBody CacheEntry cacheEntry){
        cacheService.persistInCache(cacheEntry);
        return "Product details added successfully in durable cache";
    }

}
