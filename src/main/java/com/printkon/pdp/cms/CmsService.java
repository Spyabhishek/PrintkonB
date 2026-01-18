package com.printkon.pdp.cms;

import com.printkon.pdp.cms.dto.*;
import com.printkon.pdp.cms.models.*;
import com.printkon.pdp.cms.repositories.*;

import com.printkon.pdp.common.dto.ResponseStructure;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CmsService {

	private final FeatureRepository featureRepository;
	private final PrintingServiceRepository printingServiceRepository;
	private final TestimonialRepository testimonialRepository;
	private final LandingStatRepository landingStatRepository;

	// ---------- Core Data (Above-the-fold) ----------
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<LandingPageCoreResponse>> getLandingPageCoreData() {
		List<FeatureResponse> features = featureRepository.findByEnabledTrueOrderByOrderIndexAsc().stream()
				.map(this::toFeatureDto).collect(Collectors.toList());

		List<StatResponse> stats = landingStatRepository.findByEnabledTrueOrderByOrderIndexAsc().stream()
				.map(this::toStatDto).collect(Collectors.toList());

		LandingPageCoreResponse resp = LandingPageCoreResponse.builder().features(features).stats(stats).build();

		return buildResponse("Landing page core data fetched", resp, HttpStatus.OK);
	}

	// ---------- Individual Section Methods ----------
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<FeatureResponse>>> getFeatures() {
		List<FeatureResponse> features = featureRepository.findByEnabledTrueOrderByOrderIndexAsc().stream()
				.map(this::toFeatureDto).collect(Collectors.toList());
		return buildResponse("Features fetched successfully", features, HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<TestimonialResponse>>> getTestimonials() {
		List<TestimonialResponse> testimonials = testimonialRepository.findByEnabledTrueOrderByOrderIndexAsc().stream()
				.map(this::toTestimonialDto).collect(Collectors.toList());
		return buildResponse("Testimonials fetched successfully", testimonials, HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<StatResponse>>> getStats() {
		List<StatResponse> stats = landingStatRepository.findByEnabledTrueOrderByOrderIndexAsc().stream()
				.map(this::toStatDto).collect(Collectors.toList());
		return buildResponse("Stats fetched successfully", stats, HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<PrintingServiceResponse>>> getPrintingServices() {
		List<PrintingServiceResponse> printingServices = printingServiceRepository
				.findByEnabledTrueOrderByOrderIndexAsc().stream().map(this::toPrintingServiceDto)
				.collect(Collectors.toList());
		return buildResponse("Printing services fetched successfully", printingServices, HttpStatus.OK);
	}

	// ---------- CRUD Methods (keep existing) ----------
	public ResponseEntity<ResponseStructure<FeatureResponse>> createFeature(FeatureRequest req) {
		Feature f = Feature.builder().title(req.getTitle()).description(req.getDescription()).iconUrl(req.getIconUrl())
				.orderIndex(req.getOrderIndex() == null ? 0 : req.getOrderIndex())
				.enabled(req.getEnabled() == null ? true : req.getEnabled()).build();
		Feature saved = featureRepository.save(f);
		return buildResponse("Feature created", toFeatureDto(saved), HttpStatus.CREATED);
	}

	public ResponseEntity<ResponseStructure<FeatureResponse>> updateFeature(Long id, FeatureRequest req) {
		Feature f = featureRepository.findById(id).orElseThrow(() -> new RuntimeException("Feature not found"));
		if (req.getTitle() != null)
			f.setTitle(req.getTitle());
		if (req.getDescription() != null)
			f.setDescription(req.getDescription());
		if (req.getIconUrl() != null)
			f.setIconUrl(req.getIconUrl());
		if (req.getOrderIndex() != null)
			f.setOrderIndex(req.getOrderIndex());
		if (req.getEnabled() != null)
			f.setEnabled(req.getEnabled());
		Feature saved = featureRepository.save(f);
		return buildResponse("Feature updated", toFeatureDto(saved), HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> deleteFeature(Long id) {
		featureRepository.deleteById(id);
		return buildResponse("Feature deleted", "Feature removed", HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<PrintingServiceResponse>> createPrintingService(
			PrintingServiceRequest req) {
		PrintingServiceEntity ent = PrintingServiceEntity.builder().title(req.getTitle())
				.description(req.getDescription()).iconUrl(req.getIconUrl())
				.orderIndex(req.getOrderIndex() == null ? 0 : req.getOrderIndex())
				.enabled(req.getEnabled() == null ? true : req.getEnabled()).build();
		PrintingServiceEntity saved = printingServiceRepository.save(ent);
		return buildResponse("Printing service created", toPrintingServiceDto(saved), HttpStatus.CREATED);
	}

	public ResponseEntity<ResponseStructure<TestimonialResponse>> createTestimonial(TestimonialRequest req) {
		Testimonial t = Testimonial.builder().author(req.getAuthor()).role(req.getRole()).quote(req.getQuote())
				.imageUrl(req.getImageUrl()).orderIndex(req.getOrderIndex() == null ? 0 : req.getOrderIndex())
				.enabled(req.getEnabled() == null ? true : req.getEnabled()).build();
		Testimonial saved = testimonialRepository.save(t);
		return buildResponse("Testimonial created", toTestimonialDto(saved), HttpStatus.CREATED);
	}

	public ResponseEntity<ResponseStructure<LandingStat>> createStat(StatRequest req) {
		LandingStat s = LandingStat.builder().label(req.getLabel()).value(req.getValue())
				.isDynamic(req.getIsDynamic() == null ? false : req.getIsDynamic())
				.orderIndex(req.getOrderIndex() == null ? 0 : req.getOrderIndex())
				.enabled(req.getEnabled() == null ? true : req.getEnabled()).build();
		LandingStat saved = landingStatRepository.save(s);
		return buildResponse("Stat created", saved, HttpStatus.CREATED);
	}

	// ---------- DTO Mappers ----------
	private FeatureResponse toFeatureDto(Feature f) {
		return FeatureResponse.builder().id(f.getId()).title(f.getTitle()).description(f.getDescription())
				.iconUrl(f.getIconUrl()).orderIndex(f.getOrderIndex()).enabled(f.getEnabled()).build();
	}

	private PrintingServiceResponse toPrintingServiceDto(PrintingServiceEntity e) {
		return PrintingServiceResponse.builder().id(e.getId()).title(e.getTitle()).description(e.getDescription())
				.iconUrl(e.getIconUrl()).orderIndex(e.getOrderIndex()).enabled(e.getEnabled()).build();
	}

	private TestimonialResponse toTestimonialDto(Testimonial t) {
		return TestimonialResponse.builder().id(t.getId()).author(t.getAuthor()).role(t.getRole()).quote(t.getQuote())
				.imageUrl(t.getImageUrl()).enabled(t.getEnabled()).orderIndex(t.getOrderIndex()).build();
	}

	private StatResponse toStatDto(LandingStat s) {
		return StatResponse.builder().id(s.getId()).label(s.getLabel()).value(s.getValue()).isDynamic(s.getIsDynamic())
				.enabled(s.getEnabled()).orderIndex(s.getOrderIndex()).build();
	}

	private <T> ResponseEntity<ResponseStructure<T>> buildResponse(String message, T data, HttpStatus status) {
		ResponseStructure<T> s = ResponseStructure.<T>builder().statusCode(status.value()).message(message).data(data)
				.timestamp(LocalDateTime.now()).build();
		return ResponseEntity.status(status).body(s);
	}
}