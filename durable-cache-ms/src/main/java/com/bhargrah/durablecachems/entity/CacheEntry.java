package com.bhargrah.durablecachems.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class CacheEntry {

    private String productKey;
    private String productName;


    public String getProductKey() {
        return productKey;
    }

    public String getProductName() {
        return productName;
    }
}
