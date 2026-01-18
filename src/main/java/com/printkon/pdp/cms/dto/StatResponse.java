package com.printkon.pdp.cms.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatResponse {
    private Long id;
    private String label;
    private String value;
    private Boolean isDynamic;
    private Boolean enabled;
    private Integer orderIndex;
}
