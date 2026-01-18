package com.printkon.pdp.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStockResponse {
    private String productId;
    private String name;
    private Integer currentStock;
    private Integer threshold;
    private Boolean inStock;
    private Boolean needsRestock;
}