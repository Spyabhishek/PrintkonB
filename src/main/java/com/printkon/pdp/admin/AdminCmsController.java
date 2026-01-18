package com.printkon.pdp.admin;

import com.printkon.pdp.cms.CmsService;
import com.printkon.pdp.cms.dto.FeatureResponse;
import com.printkon.pdp.cms.dto.FeatureRequest;
import com.printkon.pdp.common.dto.ResponseStructure;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cms")
@RequiredArgsConstructor
public class AdminCmsController {

	private final CmsService cmsService;

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/features")
	public ResponseEntity<ResponseStructure<FeatureResponse>> createFeature(@RequestBody FeatureRequest req) {
		return cmsService.createFeature(req);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/features/{id}")
	public ResponseEntity<ResponseStructure<FeatureResponse>> updateFeature(@PathVariable Long id,
			@RequestBody FeatureRequest req) {
		return cmsService.updateFeature(id, req);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/features/{id}")
	public ResponseEntity<ResponseStructure<String>> deleteFeature(@PathVariable Long id) {
		return cmsService.deleteFeature(id);
	}

	// Add endpoints for printingServices, testimonials, stats similarly.
}
