package com.printkon.pdp.cms.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPageCoreResponse {
	private List<FeatureResponse> features;
	private List<StatResponse> stats;
}