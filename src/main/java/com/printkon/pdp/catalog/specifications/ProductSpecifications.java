package com.printkon.pdp.catalog.specifications;

import com.printkon.pdp.catalog.models.Product;
import com.printkon.pdp.catalog.dto.ProductSearchRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecifications {

	public static Specification<Product> withSearchCriteria(ProductSearchRequest searchRequest) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			// Availability filter
			if (searchRequest.getAvailable() != null) {
				predicates.add(criteriaBuilder.equal(root.get("available"), searchRequest.getAvailable()));
			} else {
				predicates.add(criteriaBuilder.equal(root.get("available"), true));
			}

			// Search query
			if (StringUtils.hasText(searchRequest.getQuery())) {
				String searchTerm = "%" + searchRequest.getQuery().toLowerCase() + "%";
				Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchTerm);
				Predicate descriptionPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")),
						searchTerm);
				predicates.add(criteriaBuilder.or(namePredicate, descriptionPredicate));
			}

			// Category filter
			if (StringUtils.hasText(searchRequest.getCategoryId())) {
				predicates.add(
						criteriaBuilder.equal(root.get("category").get("categoryId"), searchRequest.getCategoryId()));
			}

			// Price range filter
			if (searchRequest.getMinPrice() != null) {
				predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), searchRequest.getMinPrice()));
			}
			if (searchRequest.getMaxPrice() != null) {
				predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), searchRequest.getMaxPrice()));
			}

			// Stock filter
			if (searchRequest.getInStock() != null && searchRequest.getInStock()) {
				predicates.add(criteriaBuilder.greaterThan(root.get("stockQuantity"), 0));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}
}