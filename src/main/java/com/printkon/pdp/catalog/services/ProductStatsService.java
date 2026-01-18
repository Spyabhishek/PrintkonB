package com.printkon.pdp.catalog.services;

import com.printkon.pdp.catalog.models.Product;
import com.printkon.pdp.catalog.models.ProductStats;
import com.printkon.pdp.catalog.repositories.ProductRepository;
import com.printkon.pdp.catalog.repositories.ProductStatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductStatsService {

	private final ProductStatsRepository statsRepository;
	private final ProductRepository productRepository;

	// weights - you can externalize to properties
	private static final double W_SALES = 0.6;
	private static final double W_VIEWS = 0.3;
	private static final double W_WISHLIST = 0.1;

	/**
	 * Ensure stats row exists for a product.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ProductStats ensureStatsForProduct(Product product) {
		return statsRepository.findByProductId(product.getId()).orElseGet(() -> {
			ProductStats s = ProductStats.builder().product(product).salesCount(0L).viewsCount(0L).wishlistCount(0L)
					.build();
			return statsRepository.save(s);
		});
	}

	/**
	 * Increment view counter (db-level update if exists, else create row).
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void incrementView(Long productId) {
		int updated = statsRepository.incrementViews(productId);
		if (updated == 0) {
			// No row present - create and set to 1
			Product p = productRepository.findById(productId)
					.orElseThrow(() -> new RuntimeException("Product not found"));
			ProductStats s = ProductStats.builder().product(p).viewsCount(1L).salesCount(0L).wishlistCount(0L).build();
			statsRepository.save(s);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void incrementWishlist(Long productId) {
		int updated = statsRepository.incrementWishlist(productId);
		if (updated == 0) {
			Product p = productRepository.findById(productId)
					.orElseThrow(() -> new RuntimeException("Product not found"));
			ProductStats s = ProductStats.builder().product(p).viewsCount(0L).salesCount(0L).wishlistCount(1L).build();
			statsRepository.save(s);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void incrementSales(Long productId, long qty) {
		int updated = statsRepository.incrementSales(productId, qty);
		if (updated == 0) {
			Product p = productRepository.findById(productId)
					.orElseThrow(() -> new RuntimeException("Product not found"));
			ProductStats s = ProductStats.builder().product(p).salesCount(qty).viewsCount(0L).wishlistCount(0L).build();
			statsRepository.save(s);
		}
	}

	/**
	 * Compute trending products using weighted score and return top N. This
	 * implementation fetches stats + products and computes in-memory. For heavy
	 * scale, move computation to DB or use offline batch.
	 */
	@Transactional(readOnly = true)
	public List<Product> getTrendingProducts(int limit) {
		// load all stats with products available
		List<ProductStats> allStats = statsRepository.findAll(); // small scale; consider querying only recent ones

		Map<Product, Double> scores = new HashMap<>();
		for (ProductStats s : allStats) {
			Product p = s.getProduct();
			if (p == null || !Boolean.TRUE.equals(p.getAvailable()))
				continue;

			double score = s.getSalesCount() * W_SALES + s.getViewsCount() * W_VIEWS
					+ s.getWishlistCount() * W_WISHLIST;

			// Boost if admin forced trending flag
			if (Boolean.TRUE.equals(p.getIsForceTrending())) {
				score += Double.MAX_VALUE / 4; // effectively put them on top
			}

			scores.put(p, score);
		}

		// If some products have no stats row, consider them (score 0) only if needed
		List<Product> candidates = scores.entrySet().stream()
				.sorted(Map.Entry.<Product, Double>comparingByValue().reversed()).limit(limit).map(Map.Entry::getKey)
				.collect(Collectors.toList());

		return candidates;
	}

}
