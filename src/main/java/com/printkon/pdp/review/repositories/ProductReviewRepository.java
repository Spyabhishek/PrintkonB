package com.printkon.pdp.review.repositories;

import com.printkon.pdp.review.models.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

	List<ProductReview> findByProductIdAndApprovedTrueOrderByCreatedAtDesc(Long productId);

	long countByProductIdAndApprovedTrue(Long productId);

	@Query("select avg(r.rating) from ProductReview r where r.product.id = :productId and r.approved = true")
	Double findAverageRatingByProductId(Long productId);

	List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);
}
