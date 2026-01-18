package com.printkon.pdp.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAnalyticsResponse {
    private String productId;
    private String productName;
    private Long totalSales;
    private Long totalViews;
    private Long totalWishlists;
    private BigDecimal averageRating;
    private Long reviewCount;
    private Double popularityScore;
    private Double conversionRate;
    private LocalDateTime lastUpdated;
}