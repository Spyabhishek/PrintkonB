package com.printkon.pdp.catalog.services;

import com.printkon.pdp.catalog.dto.*;
import com.printkon.pdp.catalog.models.Category;
import com.printkon.pdp.catalog.models.Product;
import com.printkon.pdp.catalog.models.ProductStats;
import com.printkon.pdp.catalog.repositories.CategoryRepository;
import com.printkon.pdp.catalog.repositories.ProductRepository;
import com.printkon.pdp.catalog.repositories.ProductStatsRepository;
import com.printkon.pdp.catalog.specifications.ProductSpecifications;
import com.printkon.pdp.common.dto.PagedResponse;
import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.exceptions.BusinessRuleException;
import com.printkon.pdp.exceptions.ResourceNotFoundException;
import com.printkon.pdp.storage.StorageService;
import com.printkon.pdp.storage.dto.ImageUploadResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@Transactional
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ProductStatsRepository productStatsRepository;
	private final ProductStatsService productStatsService;
	private final StorageService storageService;

	private static final int MAX_ID_GENERATION_ATTEMPTS = 5;

	// ------------------ CREATE OPERATIONS ------------------

	public ResponseEntity<ResponseStructure<ProductResponse>> createProduct(@Valid ProductCreateRequest request) {
		log.info("Creating product with name: {}", request.getName());

		Category category = categoryRepository.findByCategoryIdAndActiveTrue(request.getCategoryId()).orElseThrow(
				() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

		Product product = mapToProduct(request, category);

		for (int attempt = 0; attempt < MAX_ID_GENERATION_ATTEMPTS; attempt++) {
			try {
				Product savedProduct = productRepository.save(product);
				productStatsService.ensureStatsForProduct(savedProduct);

				log.info("Product created successfully with internal ID: {} and productId: {}", savedProduct.getId(),
						savedProduct.getProductId());

				return buildSuccessResponse("Product created successfully", mapToResponse(savedProduct),
						HttpStatus.CREATED);

			} catch (org.springframework.dao.DataIntegrityViolationException e) {
				if (e.getMessage().contains("product_id") && attempt < MAX_ID_GENERATION_ATTEMPTS - 1) {
					product.setProductId(generateCustomProductId());
					log.warn("Product ID collision detected, regenerating... Attempt: {}", attempt + 1);
				} else {
					throw new BusinessRuleException("Failed to create product after " + MAX_ID_GENERATION_ATTEMPTS
							+ " attempts due to ID collision");
				}
			}
		}

		throw new BusinessRuleException("Failed to create product");
	}

	// ------------------ READ OPERATIONS ------------------

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<ProductResponse>> getProductById(String productId) {
		log.info("Fetching product with ID: {}", productId);

		Product product = productRepository.findByProductIdAndAvailableTrue(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		incrementViewCountAsync(product.getId());

		log.info("Successfully fetched product: {} (ID: {})", product.getName(), productId);
		return buildSuccessResponse("Product fetched successfully", mapToResponse(product), HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<ProductResponse>> getProductByInternalId(Long internalId) {
		log.info("Fetching product with internal ID: {}", internalId);

		Product product = productRepository.findByIdAndAvailableTrue(internalId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with internal ID: " + internalId));

		incrementViewCountAsync(product.getId());

		return buildSuccessResponse("Product fetched successfully", mapToResponse(product), HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<PagedResponse<ProductResponse>>> getAllProducts(String categoryId,
			Integer page, Integer size, String sortBy, String sortDirection) {

		log.info("Fetching products with categoryId: {}, page: {}, size: {}", categoryId, page, size);

		Pageable pageable = createPageable(page, size, sortBy, sortDirection);
		Page<Product> productsPage;

		if (categoryId != null) {
			productsPage = productRepository.findByCategoryIdAndAvailableTrue(categoryId, pageable);
		} else {
			productsPage = productRepository.findByAvailableTrue(pageable);
		}

		PagedResponse<ProductResponse> pagedResponse = createPagedResponse(productsPage);
		log.info("Successfully fetched {} products", pagedResponse.getContent().size());

		return buildSuccessResponse("Products fetched successfully", pagedResponse, HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> getProductsByIds(List<String> productIds) {
		log.info("Fetching products by IDs: {}", productIds);

		List<Product> products = productIds.stream()
				.map(productId -> productRepository.findByProductIdAndAvailableTrue(productId).orElse(null))
				.filter(product -> product != null).collect(Collectors.toList());

		List<ProductResponse> responses = products.stream().map(this::mapToResponse).collect(Collectors.toList());

		log.info("Found {} products out of {} requested", responses.size(), productIds.size());
		return buildSuccessResponse("Products fetched successfully", responses, HttpStatus.OK);
	}

	// ------------------ SEARCH OPERATIONS ------------------

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<PagedResponse<ProductResponse>>> searchProducts(
			@Valid ProductSearchRequest searchRequest) {

		log.info("Searching products with criteria: {}", searchRequest);

		Pageable pageable = createPageable(searchRequest.getPage(), searchRequest.getSize(), searchRequest.getSortBy(),
				searchRequest.getSortDirection());

		Specification<Product> spec = ProductSpecifications.withSearchCriteria(searchRequest);
		Page<Product> productsPage = productRepository.findAll(spec, pageable);

		PagedResponse<ProductResponse> pagedResponse = createPagedResponse(productsPage);
		log.info("Search completed - found {} products", pagedResponse.getContent().size());

		return buildSuccessResponse("Search completed successfully", pagedResponse, HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> searchProductsSimple(String query) {
		log.info("Simple search for products with query: '{}'", query);

		if (query == null || query.trim().isEmpty()) {
			return buildSuccessResponse("Please provide a search query", new ArrayList<>(), HttpStatus.BAD_REQUEST);
		}

		List<Product> products = productRepository.searchByNameOrDescription(query.trim());
		List<ProductResponse> responses = products.stream().map(this::mapToResponse).collect(Collectors.toList());

		log.info("Simple search found {} products for query: '{}'", responses.size(), query);
		return buildSuccessResponse("Search completed successfully", responses, HttpStatus.OK);
	}

	// ------------------ UPDATE OPERATIONS ------------------

	public ResponseEntity<ResponseStructure<ProductResponse>> updateProduct(String productId,
			@Valid ProductUpdateRequest request) {
		log.info("Updating product with ID: {}", productId);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		boolean hasChanges = updateProductFields(product, request);

		if (hasChanges) {
			Product savedProduct = productRepository.save(product);
			log.info("Product updated successfully: {}", productId);
			return buildSuccessResponse("Product updated successfully", mapToResponse(savedProduct), HttpStatus.OK);
		} else {
			log.info("No changes detected for product: {}", productId);
			return buildSuccessResponse("No changes detected", mapToResponse(product), HttpStatus.OK);
		}
	}

	public ResponseEntity<ResponseStructure<ProductResponse>> partialUpdateProduct(String productId,
			Map<String, Object> updates) {
		log.info("Partial update for product: {}, updates: {}", productId, updates);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		boolean hasChanges = applyPartialUpdates(product, updates);

		if (hasChanges) {
			Product savedProduct = productRepository.save(product);
			log.info("Product partially updated successfully: {}", productId);
			return buildSuccessResponse("Product updated successfully", mapToResponse(savedProduct), HttpStatus.OK);
		} else {
			log.info("No changes detected for product: {}", productId);
			return buildSuccessResponse("No changes detected", mapToResponse(product), HttpStatus.OK);
		}
	}

	// ------------------ DELETE OPERATIONS ------------------

	public ResponseEntity<ResponseStructure<Void>> deleteProduct(String productId) {
		log.info("Soft deleting product with ID: {}", productId);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		if (!product.getAvailable()) {
			throw new BusinessRuleException("Product is already deleted");
		}

		product.setAvailable(false);
		productRepository.save(product);

		log.info("Product soft deleted successfully: {}", productId);
		return buildSuccessResponse("Product deleted successfully", null, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<Void>> hardDeleteProduct(String productId) {
		log.info("Hard deleting product with ID: {}", productId);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		// Delete associated images from storage
		deleteProductImages(product);

		productRepository.delete(product);

		log.info("Product hard deleted successfully: {}", productId);
		return buildSuccessResponse("Product permanently deleted", null, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<Void>> deleteProductsBulk(List<String> productIds) {
		log.info("Bulk deleting {} products", productIds.size());

		List<Product> products = productRepository.findAllByProductIdIn(productIds);

		if (products.size() != productIds.size()) {
			throw new BusinessRuleException("Some products not found");
		}

		products.forEach(product -> product.setAvailable(false));
		productRepository.saveAll(products);

		log.info("Bulk soft delete completed for {} products", products.size());
		return buildSuccessResponse("Products deleted successfully", null, HttpStatus.OK);
	}

	// ------------------ STOCK MANAGEMENT ------------------

	public ResponseEntity<ResponseStructure<ProductResponse>> updateStock(String productId, Integer quantity) {
		log.info("Updating stock for product: {}, quantity: {}", productId, quantity);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		if (quantity < 0) {
			throw new BusinessRuleException("Stock quantity cannot be negative");
		}

		product.setStockQuantity(quantity);
		Product savedProduct = productRepository.save(product);

		log.info("Stock updated for product: {}, new quantity: {}", productId, quantity);
		return buildSuccessResponse("Stock updated successfully", mapToResponse(savedProduct), HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<ProductResponse>> reduceStock(String productId, Integer quantity) {
		log.info("Reducing stock for product: {}, quantity: {}", productId, quantity);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		try {
			product.reduceStock(quantity);
			Product savedProduct = productRepository.save(product);

			log.info("Stock reduced for product: {}, remaining quantity: {}", productId,
					savedProduct.getStockQuantity());
			return buildSuccessResponse("Stock reduced successfully", mapToResponse(savedProduct), HttpStatus.OK);
		} catch (IllegalStateException e) {
			throw new BusinessRuleException(e.getMessage());
		}
	}

	public ResponseEntity<ResponseStructure<ProductResponse>> addStock(String productId, Integer quantity) {
		log.info("Adding stock for product: {}, quantity: {}", productId, quantity);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		try {
			product.addStock(quantity);
			Product savedProduct = productRepository.save(product);

			log.info("Stock added for product: {}, new quantity: {}", productId, savedProduct.getStockQuantity());
			return buildSuccessResponse("Stock added successfully", mapToResponse(savedProduct), HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			throw new BusinessRuleException(e.getMessage());
		}
	}

	public ResponseEntity<ResponseStructure<List<ProductStockResponse>>> getLowStockProducts(Integer threshold) {
		log.info("Fetching products with stock below threshold: {}", threshold);

		int stockThreshold = threshold != null ? threshold : 10;

		List<Product> lowStockProducts = productRepository.findAll().stream()
				.filter(product -> product.getAvailable() && product.getStockQuantity() <= stockThreshold)
				.collect(Collectors.toList());

		List<ProductStockResponse> responses = lowStockProducts.stream()
				.map(product -> ProductStockResponse.builder().productId(product.getProductId()).name(product.getName())
						.currentStock(product.getStockQuantity()).threshold(stockThreshold).inStock(product.isInStock())
						.build())
				.collect(Collectors.toList());

		log.info("Found {} products with low stock", responses.size());
		return buildSuccessResponse("Low stock products fetched", responses, HttpStatus.OK);
	}

	// ------------------ POPULAR & TRENDING PRODUCTS ------------------

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> getPopularProducts(Integer limit) {
		log.info("Fetching popular products with limit: {}", limit);

		int productLimit = limit != null && limit > 0 ? Math.min(limit, 50) : 10;

		List<Product> popularProducts = productRepository.findByIsPopularTrueAndAvailableTrue().stream()
				.limit(productLimit).collect(Collectors.toList());

		List<ProductResponse> responses = popularProducts.stream().map(this::mapToResponse)
				.collect(Collectors.toList());

		log.info("Found {} popular products", responses.size());
		return buildSuccessResponse("Popular products fetched successfully", responses, HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> getTrendingProducts(Integer limit) {
		log.info("Fetching trending products with limit: {}", limit);

		int productLimit = limit != null && limit > 0 ? Math.min(limit, 50) : 10;

		List<Product> trendingProducts = productStatsService.getTrendingProducts(productLimit);

		List<ProductResponse> responses = trendingProducts.stream().map(this::mapToResponse)
				.collect(Collectors.toList());

		log.info("Found {} trending products", responses.size());
		return buildSuccessResponse("Trending products fetched successfully", responses, HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> getForceTrendingProducts() {
		log.info("Fetching force-trending products");

		List<Product> forceTrendingProducts = productRepository.findByIsForceTrendingTrueAndAvailableTrue();

		List<ProductResponse> responses = forceTrendingProducts.stream().map(this::mapToResponse)
				.collect(Collectors.toList());

		log.info("Found {} force-trending products", responses.size());
		return buildSuccessResponse("Force-trending products fetched successfully", responses, HttpStatus.OK);
	}

	// ------------------ ADMIN OPERATIONS ------------------

	public ResponseEntity<ResponseStructure<ProductResponse>> setProductPopular(String productId, Boolean popular) {
		log.info("Setting product popularity: {} -> {}", productId, popular);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		product.setIsPopular(Boolean.TRUE.equals(popular));
		Product savedProduct = productRepository.save(product);

		String message = Boolean.TRUE.equals(popular) ? "Product marked as popular" : "Product unmarked as popular";

		log.info("Product popularity updated: {} -> {}", productId, popular);
		return buildSuccessResponse(message, mapToResponse(savedProduct), HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<ProductResponse>> setProductForceTrending(String productId,
			Boolean forceTrending) {
		log.info("Setting product force-trending: {} -> {}", productId, forceTrending);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		product.setIsForceTrending(Boolean.TRUE.equals(forceTrending));
		Product savedProduct = productRepository.save(product);

		String message = Boolean.TRUE.equals(forceTrending) ? "Product force-marked as trending"
				: "Product force-trending removed";

		log.info("Product force-trending updated: {} -> {}", productId, forceTrending);
		return buildSuccessResponse(message, mapToResponse(savedProduct), HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<List<ProductResponse>>> getOutOfStockProducts() {
		log.info("Fetching out-of-stock products");

		List<Product> outOfStockProducts = productRepository.findAll().stream()
				.filter(product -> product.getAvailable() && !product.isInStock()).collect(Collectors.toList());

		List<ProductResponse> responses = outOfStockProducts.stream().map(this::mapToResponse)
				.collect(Collectors.toList());

		log.info("Found {} out-of-stock products", responses.size());
		return buildSuccessResponse("Out-of-stock products fetched", responses, HttpStatus.OK);
	}

	// ------------------ IMAGE MANAGEMENT ------------------

	public ResponseEntity<ResponseStructure<ProductResponse>> uploadProductImages(String productId,
			MultipartFile[] files, boolean setMainImage) {
		log.info("Uploading {} images to product: {}, setMainImage: {}", files.length, productId, setMainImage);

		if (files == null || files.length == 0) {
			throw new BusinessRuleException("No files provided for upload");
		}

		try {
			List<String> uploadedUrls = new ArrayList<>();
			for (MultipartFile file : files) {
				validateImageFile(file);
				ImageUploadResponse uploadResponse = storageService.store(file, "products");
				uploadedUrls.add(uploadResponse.getImageUrl());
			}

			return addImagesToProduct(productId, uploadedUrls, setMainImage);

		} catch (IOException e) {
			log.error("Failed to upload images for product: {} - Error: {}", productId, e.getMessage(), e);
			throw new BusinessRuleException("Failed to upload images: " + e.getMessage());
		}
	}

	public ResponseEntity<ResponseStructure<ProductResponse>> removeProductImage(String productId, String imageUrl) {
		log.info("Removing image from product: {}, imageUrl: {}", productId, imageUrl);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		if (!product.getImageUrls().contains(imageUrl)) {
			throw new BusinessRuleException("Image URL not found in product's image list");
		}

		// Delete from storage
		boolean deletedFromStorage = storageService.delete(imageUrl);
		if (!deletedFromStorage) {
			log.warn("Failed to delete image from storage: {}, but continuing to remove from product", imageUrl);
		}

		// Remove from product
		product.removeImageUrl(imageUrl);
		Product savedProduct = productRepository.save(product);

		log.info("Image removed from product: {}", productId);
		return buildSuccessResponse("Image removed successfully", mapToResponse(savedProduct), HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<ProductResponse>> setMainImage(String productId, String imageUrl) {
		log.info("Setting main image for product: {}, imageUrl: {}", productId, imageUrl);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		if (!product.getImageUrls().contains(imageUrl)) {
			throw new BusinessRuleException("Image URL not found in product's image list");
		}

		product.setMainImageUrl(imageUrl);
		Product savedProduct = productRepository.save(product);

		log.info("Main image set for product: {}", productId);
		return buildSuccessResponse("Main image updated successfully", mapToResponse(savedProduct), HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<ProductResponse>> reorderImages(String productId, List<String> imageUrls) {
		log.info("Reordering images for product: {}", productId);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		// Validate that all provided URLs exist in the product
		if (!product.getImageUrls().containsAll(imageUrls) || !imageUrls.containsAll(product.getImageUrls())) {
			throw new BusinessRuleException("Provided image URLs don't match product's current images");
		}

		product.setImageUrls(new ArrayList<>(imageUrls));

		// Update main image if it's the first one
		if (!imageUrls.isEmpty() && !imageUrls.get(0).equals(product.getMainImageUrl())) {
			product.setMainImageUrl(imageUrls.get(0));
		}

		Product savedProduct = productRepository.save(product);

		log.info("Images reordered for product: {}", productId);
		return buildSuccessResponse("Images reordered successfully", mapToResponse(savedProduct), HttpStatus.OK);
	}

	// ------------------ BULK OPERATIONS ------------------

	public ResponseEntity<ResponseStructure<BulkOperationResult>> bulkUpdateAvailability(List<String> productIds,
			Boolean available) {
		log.info("Bulk updating availability for {} products to: {}", productIds.size(), available);

		List<Product> products = productRepository.findAllByProductIdIn(productIds);

		if (products.isEmpty()) {
			throw new ResourceNotFoundException("No products found with the provided IDs");
		}

		products.forEach(product -> product.setAvailable(available));
		productRepository.saveAll(products);

		BulkOperationResult result = BulkOperationResult.builder().totalProcessed(products.size())
				.successfulOperations(products.size()).failedOperations(0)
				.message("Availability updated for " + products.size() + " products").build();

		log.info("Bulk availability update completed: {}", result);
		return buildSuccessResponse("Bulk update completed", result, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<BulkOperationResult>> bulkUpdatePrices(
			Map<String, BigDecimal> productPriceUpdates) {
		log.info("Bulk updating prices for {} products", productPriceUpdates.size());

		List<Product> products = productRepository.findAllByProductIdIn(new ArrayList<>(productPriceUpdates.keySet()));

		if (products.size() != productPriceUpdates.size()) {
			throw new BusinessRuleException("Some products not found for price update");
		}

		products.forEach(product -> {
			BigDecimal newPrice = productPriceUpdates.get(product.getProductId());
			if (newPrice != null && newPrice.compareTo(BigDecimal.ZERO) > 0) {
				product.setPrice(newPrice);
			}
		});

		productRepository.saveAll(products);

		BulkOperationResult result = BulkOperationResult.builder().totalProcessed(products.size())
				.successfulOperations(products.size()).failedOperations(0)
				.message("Prices updated for " + products.size() + " products").build();

		log.info("Bulk price update completed: {}", result);
		return buildSuccessResponse("Bulk price update completed", result, HttpStatus.OK);
	}

	// ------------------ ANALYTICS & REPORTING ------------------

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<ProductAnalyticsResponse>> getProductAnalytics(String productId) {
		log.info("Fetching analytics for product: {}", productId);

		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		Optional<ProductStats> statsOpt = productStatsRepository.findByProductId(product.getId());

		ProductAnalyticsResponse analytics = ProductAnalyticsResponse.builder().productId(productId)
				.productName(product.getName()).totalSales(statsOpt.map(ProductStats::getSalesCount).orElse(0L))
				.totalViews(statsOpt.map(ProductStats::getViewsCount).orElse(0L))
				.totalWishlists(statsOpt.map(ProductStats::getWishlistCount).orElse(0L))
				.averageRating(statsOpt.map(ProductStats::getAverageRating).orElse(BigDecimal.ZERO))
				.reviewCount(statsOpt.map(ProductStats::getReviewCount).orElse(0L))
				.popularityScore(statsOpt.map(ProductStats::getPopularityScore).orElse(0.0))
				.conversionRate(calculateConversionRate(statsOpt))
				.lastUpdated(statsOpt.map(ProductStats::getLastUpdated).orElse(null)).build();

		return buildSuccessResponse("Product analytics fetched", analytics, HttpStatus.OK);
	}

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<List<ProductSummaryResponse>>> getProductsSummary() {
		log.info("Fetching products summary");

		long totalProducts = productRepository.count();
		long availableProducts = productRepository.countAvailableProducts();
		long outOfStockProducts = productRepository.findAll().stream()
				.filter(product -> product.getAvailable() && !product.isInStock()).count();
		long popularProducts = productRepository.findByIsPopularTrueAndAvailableTrue().size();

		List<ProductSummaryResponse> summary = List.of(
				ProductSummaryResponse.builder().type("TOTAL_PRODUCTS").count(totalProducts)
						.description("Total products in system").build(),
				ProductSummaryResponse.builder().type("AVAILABLE_PRODUCTS").count(availableProducts)
						.description("Products currently available").build(),
				ProductSummaryResponse.builder().type("OUT_OF_STOCK").count(outOfStockProducts)
						.description("Available products with zero stock").build(),
				ProductSummaryResponse.builder().type("POPULAR_PRODUCTS").count((long) popularProducts)
						.description("Products marked as popular").build());

		return buildSuccessResponse("Products summary fetched", summary, HttpStatus.OK);
	}

	// ------------------ PRIVATE HELPER METHODS ------------------

	private Product mapToProduct(ProductCreateRequest request, Category category) {
		return Product.builder().name(request.getName()).description(request.getDescription()).price(request.getPrice())
				.available(request.getAvailable())
				.stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
				.sku(request.getSku()).category(category)
				.imageUrls(request.getImageUrls() != null ? request.getImageUrls() : new ArrayList<>())
				.mainImageUrl(request.getMainImageUrl()).build();
	}

	private ProductResponse mapToResponse(Product product) {
		Optional<ProductStats> stats = productStatsRepository.findByProductId(product.getId());

		ProductStatsResponse statsResponse = null;
		if (stats.isPresent()) {
			ProductStats stat = stats.get();
			statsResponse = ProductStatsResponse.builder().salesCount(stat.getSalesCount())
					.viewsCount(stat.getViewsCount()).wishlistCount(stat.getWishlistCount())
					.reviewCount(stat.getReviewCount()).averageRating(stat.getAverageRating())
					.popularityScore(stat.getPopularityScore()).lastUpdated(stat.getLastUpdated()).build();
		}

		return ProductResponse.builder().productId(product.getProductId()).name(product.getName())
				.description(product.getDescription()).price(product.getPrice()).available(product.getAvailable())
				.stockQuantity(product.getStockQuantity()).sku(product.getSku())
				.categoryName(product.getCategory().getName()).categoryId(product.getCategory().getCategoryId())
				.imageUrls(product.getImageUrls()).mainImageUrl(product.getMainImageUrl())
				.createdAt(product.getCreatedAt()).updatedAt(product.getUpdatedAt()).inStock(product.isInStock())
				.isPopular(product.getIsPopular()).isForceTrending(product.getIsForceTrending()).stats(statsResponse)
				.build();
	}

	private boolean updateProductFields(Product product, ProductUpdateRequest request) {
		boolean hasChanges = false;

		if (request.getName() != null && !request.getName().equals(product.getName())) {
			product.setName(request.getName());
			hasChanges = true;
		}
		if (request.getDescription() != null && !request.getDescription().equals(product.getDescription())) {
			product.setDescription(request.getDescription());
			hasChanges = true;
		}
		if (request.getPrice() != null && !request.getPrice().equals(product.getPrice())) {
			product.setPrice(request.getPrice());
			hasChanges = true;
		}
		if (request.getAvailable() != null && !request.getAvailable().equals(product.getAvailable())) {
			product.setAvailable(request.getAvailable());
			hasChanges = true;
		}
		if (request.getStockQuantity() != null && !request.getStockQuantity().equals(product.getStockQuantity())) {
			product.setStockQuantity(request.getStockQuantity());
			hasChanges = true;
		}
		if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
			product.setSku(request.getSku());
			hasChanges = true;
		}
		if (request.getCategoryId() != null && !product.getCategory().getCategoryId().equals(request.getCategoryId())) {
			Category category = categoryRepository.findByCategoryIdAndActiveTrue(request.getCategoryId()).orElseThrow(
					() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));
			product.setCategory(category);
			hasChanges = true;
		}

		return hasChanges;
	}

	private boolean applyPartialUpdates(Product product, Map<String, Object> updates) {
		boolean hasChanges = false;

		for (Map.Entry<String, Object> entry : updates.entrySet()) {
			switch (entry.getKey()) {
			case "name":
				String name = (String) entry.getValue();
				if (name != null && !name.equals(product.getName())) {
					product.setName(name);
					hasChanges = true;
				}
				break;
			case "price":
				BigDecimal price = new BigDecimal(entry.getValue().toString());
				if (price != null && !price.equals(product.getPrice())) {
					product.setPrice(price);
					hasChanges = true;
				}
				break;
			case "stockQuantity":
				Integer stockQuantity = (Integer) entry.getValue();
				if (stockQuantity != null && !stockQuantity.equals(product.getStockQuantity())) {
					product.setStockQuantity(stockQuantity);
					hasChanges = true;
				}
				break;
			case "available":
				Boolean available = (Boolean) entry.getValue();
				if (available != null && !available.equals(product.getAvailable())) {
					product.setAvailable(available);
					hasChanges = true;
				}
				break;
			// Add more fields as needed
			}
		}

		return hasChanges;
	}

	private void incrementViewCountAsync(Long productId) {
		try {
			productStatsService.incrementView(productId);
		} catch (Exception ex) {
			log.warn("Failed to increment view count for product internal ID {}: {}", productId, ex.getMessage());
		}
	}

	private void deleteProductImages(Product product) {
		if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
			product.getImageUrls().forEach(imageUrl -> {
				try {
					storageService.delete(imageUrl);
				} catch (Exception e) {
					log.warn("Failed to delete image from storage: {}", imageUrl, e);
				}
			});
		}
	}

	private void validateImageFile(MultipartFile file) {
		if (file.isEmpty()) {
			throw new BusinessRuleException("File is empty");
		}

		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new BusinessRuleException("File must be an image");
		}

		if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
			throw new BusinessRuleException("File size must be less than 10MB");
		}
	}

	private ResponseEntity<ResponseStructure<ProductResponse>> addImagesToProduct(String productId,
			List<String> imageUrls, boolean setMainImage) {
		Product product = productRepository.findByProductId(productId)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

		imageUrls.forEach(product::addImageUrl);

		if (setMainImage && !imageUrls.isEmpty()) {
			product.setMainImageUrl(imageUrls.get(0));
		}

		Product savedProduct = productRepository.save(product);
		log.info("Successfully added {} images to product: {}", imageUrls.size(), productId);

		return buildSuccessResponse("Images added successfully", mapToResponse(savedProduct), HttpStatus.OK);
	}

	private PagedResponse<ProductResponse> createPagedResponse(Page<Product> productsPage) {
		List<ProductResponse> content = productsPage.getContent().stream().map(this::mapToResponse)
				.collect(Collectors.toList());

		return PagedResponse.<ProductResponse>builder().content(content).pageNumber(productsPage.getNumber())
				.pageSize(productsPage.getSize()).totalElements(productsPage.getTotalElements())
				.totalPages(productsPage.getTotalPages()).last(productsPage.isLast()).first(productsPage.isFirst())
				.build();
	}

	private Pageable createPageable(Integer page, Integer size, String sortBy, String sortDirection) {
		int pageNumber = page != null && page >= 0 ? page : 0;
		int pageSize = size != null && size > 0 ? Math.min(size, 100) : 20;

		Sort.Direction direction = Sort.Direction
				.fromString(sortDirection != null ? sortDirection.toUpperCase() : "DESC");
		String sortField = sortBy != null ? sortBy : "createdAt";

		Sort sort = Sort.by(direction, sortField);
		return PageRequest.of(pageNumber, pageSize, sort);
	}

	private Double calculateConversionRate(Optional<ProductStats> stats) {
		if (stats.isPresent()) {
			ProductStats stat = stats.get();
			if (stat.getViewsCount() > 0) {
				return (double) stat.getSalesCount() / stat.getViewsCount();
			}
		}
		return 0.0;
	}

	private String generateCustomProductId() {
		String timestamp = String.valueOf(System.currentTimeMillis() % 1000000);
		String random = String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
		return "P" + timestamp + random;
	}

	private <T> ResponseEntity<ResponseStructure<T>> buildSuccessResponse(String message, T data, HttpStatus status) {
		ResponseStructure<T> structure = ResponseStructure.<T>builder().success(true).statusCode(status.value())
				.message(message).data(data).timestamp(LocalDateTime.now()).build();
		return ResponseEntity.status(status).body(structure);
	}
}