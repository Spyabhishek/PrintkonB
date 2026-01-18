package com.printkon.pdp.cms.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureResponse {
    private Long id;
    private String title;
    private String description;
    private String iconUrl;
    private Integer orderIndex;
    private Boolean enabled;
}
