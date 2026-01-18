package com.printkon.pdp.cms;

import com.printkon.pdp.cms.dto.PrintingServiceRequest;
import com.printkon.pdp.cms.dto.PrintingServiceResponse;
import com.printkon.pdp.common.dto.ResponseStructure;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/landing/printing-services")
@RequiredArgsConstructor
public class PrintingServicesController {

	private final CmsService cmsService;

	@GetMapping
	public ResponseEntity<ResponseStructure<List<PrintingServiceResponse>>> getPrintingServices() {
		return cmsService.getPrintingServices();
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<PrintingServiceResponse>> createPrintingService(
			@RequestBody PrintingServiceRequest request) {
		return cmsService.createPrintingService(request);
	}
}