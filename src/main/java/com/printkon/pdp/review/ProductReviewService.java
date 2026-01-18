package com.printkon.pdp.review;

import com.printkon.pdp.catalog.models.Product;
import com.printkon.pdp.catalog.repositories.ProductRepository;
import com.printkon.pdp.review.dto.ProductReviewRequest;
import com.printkon.pdp.review.dto.ProductReviewResponse;
import com.printkon.pdp.review.models.ProductReview;
import com.printkon.pdp.review.repositories.ProductReviewRepository;
import com.printkon.pdp.user.models.User;
import com.printkon.pdp.user.repositories.UserRepository;
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
public class ProductReviewService {

	private final ProductReviewRepository reviewRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	public ResponseEntity<ResponseStructure<ProductReviewResponse>> submitReview(Long productId,
			ProductReviewRequest req, Long currentUserId) {
		Product p = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));
		User user = null;
		if (currentUserId != null) {
			user = userRepository.findById(currentUserId).orElse(null);
		}

		ProductReview review = ProductReview.builder().product(p).user(user).rating(req.getRating())
				.title(req.getTitle()).comment(req.getComment()).approved(false) // moderation required
				.build();
		ProductReview saved = reviewRepository.save(review);

		return buildResponse("Review submitted (awaiting moderation)", toDto(saved), HttpStatus.CREATED);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<ProductReviewResponse>>> getApprovedReviews(Long productId) {
		List<ProductReview> list = reviewRepository.findByProductIdAndApprovedTrueOrderByCreatedAtDesc(productId);
		List<ProductReviewResponse> resp = list.stream().map(this::toDto).collect(Collectors.toList());
		return buildResponse("Reviews fetched", resp, HttpStatus.OK);
	}

	// Admin: list all reviews for a product (including unapproved)
	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<ProductReviewResponse>>> getAllReviewsForProduct(Long productId) {
		List<ProductReview> list = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
		return buildResponse("All reviews fetched", list.stream().map(this::toDto).collect(Collectors.toList()),
				HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<ProductReviewResponse>> approveReview(Long reviewId, Boolean approved) {
		ProductReview review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new RuntimeException("Review not found"));
		review.setApproved(Boolean.TRUE.equals(approved));
		ProductReview saved = reviewRepository.save(review);
		return buildResponse("Review moderation updated", toDto(saved), HttpStatus.OK);
	}

	public Double getAverageRating(Long productId) {
		Double avg = reviewRepository.findAverageRatingByProductId(productId);
		return avg == null ? 0.0 : avg;
	}

	public long getReviewCount(Long productId) {
		return reviewRepository.countByProductIdAndApprovedTrue(productId);
	}

	private ProductReviewResponse toDto(ProductReview r) {
		return ProductReviewResponse.builder().id(r.getId())
				.productId(r.getProduct() != null ? r.getProduct().getId() : null)
				.userId(r.getUser() != null ? r.getUser().getId() : null).rating(r.getRating()).title(r.getTitle())
				.comment(r.getComment()).approved(r.getApproved()).createdAt(r.getCreatedAt()).build();
	}

	private <T> ResponseEntity<ResponseStructure<T>> buildResponse(String message, T data, HttpStatus status) {
		ResponseStructure<T> s = ResponseStructure.<T>builder().statusCode(status.value()).message(message).data(data)
				.timestamp(LocalDateTime.now()).build();
		return ResponseEntity.status(status).body(s);
	}
}
