package com.printkon.pdp.cms.dto;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    private String iconUrl;
    private Integer orderIndex;
    private Boolean enabled;
}
