package com.printkon.pdp.catalog.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResult {
    private Integer totalProcessed;
    private Integer successfulOperations;
    private Integer failedOperations;
    private String message;
    private LocalDateTime processedAt;
}