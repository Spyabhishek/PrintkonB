package com.printkon.pdp.cms;

import com.printkon.pdp.cms.dto.StatRequest;
import com.printkon.pdp.cms.dto.StatResponse;
import com.printkon.pdp.cms.models.LandingStat;
import com.printkon.pdp.common.dto.ResponseStructure;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/landing/stats")
@RequiredArgsConstructor
public class StatsController {

	private final CmsService cmsService;

	@GetMapping
	public ResponseEntity<ResponseStructure<List<StatResponse>>> getStats() {
		return cmsService.getStats();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<LandingStat>> createStat(@RequestBody StatRequest request) {
		return cmsService.createStat(request);
	}
}