package com.printkon.pdp.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private String productId;
    private String name;
    private String description;
    private BigDecimal price;
    private Boolean available;
    private Integer stockQuantity;
    private String sku;
    private String categoryName;
    private String categoryId;
    private List<String> imageUrls;
    private String mainImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean inStock;
    
    // ADD THESE MISSING FIELDS:
    private Boolean isPopular;
    private Boolean isForceTrending;
    
    private ProductStatsResponse stats;
}