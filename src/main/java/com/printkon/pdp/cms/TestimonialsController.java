package com.printkon.pdp.cms;

import com.printkon.pdp.cms.dto.TestimonialRequest;
import com.printkon.pdp.cms.dto.TestimonialResponse;
import com.printkon.pdp.common.dto.ResponseStructure;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/landing/testimonials")
@RequiredArgsConstructor
public class TestimonialsController {

	private final CmsService cmsService;

	@GetMapping
	public ResponseEntity<ResponseStructure<List<TestimonialResponse>>> getTestimonials() {
		return cmsService.getTestimonials();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<TestimonialResponse>> createTestimonial(
			@RequestBody TestimonialRequest request) {
		return cmsService.createTestimonial(request);
	}
}