package com.printkon.pdp.cms;

import com.printkon.pdp.cms.dto.FeatureRequest;
import com.printkon.pdp.cms.dto.FeatureResponse;
import com.printkon.pdp.common.dto.ResponseStructure;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/landing/features")
@RequiredArgsConstructor
public class FeaturesController {

	private final CmsService cmsService;

	@GetMapping
	public ResponseEntity<ResponseStructure<List<FeatureResponse>>> getFeatures() {
		return cmsService.getFeatures();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<FeatureResponse>> createFeature(@RequestBody FeatureRequest request) {
		return cmsService.createFeature(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<FeatureResponse>> updateFeature(@PathVariable Long id,
			@RequestBody FeatureRequest request) {
		return cmsService.updateFeature(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<String>> deleteFeature(@PathVariable Long id) {
		return cmsService.deleteFeature(id);
	}
}