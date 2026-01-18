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
public class ProductStatsResponse {
    private Long salesCount;
    private Long viewsCount;
    private Long wishlistCount;
    private Long reviewCount;
    private BigDecimal averageRating;
    private Double popularityScore;
    private LocalDateTime lastUpdated;
}
