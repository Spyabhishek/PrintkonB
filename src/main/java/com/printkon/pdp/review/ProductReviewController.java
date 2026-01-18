package com.printkon.pdp.review;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.review.dto.ProductReviewRequest;
import com.printkon.pdp.review.dto.ProductReviewResponse;
import com.printkon.pdp.user.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

	private final ProductReviewService reviewService;

	@PostMapping
	public ResponseEntity<ResponseStructure<ProductReviewResponse>> submitReview(@PathVariable Long productId,
			@Valid @RequestBody ProductReviewRequest req, @AuthenticationPrincipal UserDetailsImpl user) {

		Long userId = user != null ? user.getId() : null;
		return reviewService.submitReview(productId, req, userId);
	}

	@GetMapping
	public ResponseEntity<ResponseStructure<List<ProductReviewResponse>>> getApprovedReviews(
			@PathVariable Long productId) {
		return reviewService.getApprovedReviews(productId);
	}

	// Admin endpoints
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/all")
	public ResponseEntity<ResponseStructure<List<ProductReviewResponse>>> getAllReviews(@PathVariable Long productId) {
		return reviewService.getAllReviewsForProduct(productId);
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PutMapping("/{reviewId}/approve")
	public ResponseEntity<ResponseStructure<ProductReviewResponse>> approveReview(@PathVariable Long productId,
			@PathVariable Long reviewId, @RequestParam Boolean approve) {
		return reviewService.approveReview(reviewId, approve);
	}
}
