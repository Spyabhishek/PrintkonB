package com.printkon.pdp.cms.dto;

import com.printkon.pdp.catalog.dto.ProductResponse;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LandingPageResponse {
	private List<FeatureResponse> features;
	private List<PrintingServiceResponse> printingServices;
	private List<TestimonialResponse> testimonials;
	private List<StatResponse> stats;
	private List<ProductResponse> popularProducts;
	private List<ProductResponse> trendingProducts;
}
