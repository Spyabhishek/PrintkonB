package com.printkon.pdp.catalog;

import com.printkon.pdp.catalog.dto.*;
import com.printkon.pdp.catalog.services.ProductService;
import com.printkon.pdp.common.dto.PagedResponse;
import com.printkon.pdp.common.dto.ResponseStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/products")
@CrossOrigin("*")
@RequiredArgsConstructor
@Validated
public class ProductController {

	private final ProductService productService;

	// ------------------ PUBLIC ENDPOINTS ------------------
	@GetMapping
	public ResponseEntity<ResponseStructure<PagedResponse<ProductResponse>>> getAllProducts(
			@RequestParam(required = false) String categoryId, @RequestParam(defaultValue = "0") Integer page,
			@RequestParam(defaultValue = "20") Integer size, @RequestParam(defaultValue = "createdAt") String sortBy,
			@RequestParam(defaultValue = "DESC") String sortDirection) {

		return productService.getAllProducts(categoryId, page, size, sortBy, sortDirection);
	}

	@GetMapping("/{productId}")
	public ResponseEntity<ResponseStructure<ProductResponse>> getProductById(@PathVariable @NotBlank String productId) {
		return productService.getProductById(productId);
	}

	@GetMapping("/search")
	public ResponseEntity<ResponseStructure<PagedResponse<ProductResponse>>> searchProducts(
			@Valid ProductSearchRequest searchRequest) {
		return productService.searchProducts(searchRequest);
	}

	@GetMapping("/search/simple")
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> searchProductsSimple(
			@RequestParam @NotBlank String query) {
		return productService.searchProductsSimple(query);
	}

	@GetMapping("/popular")
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> getPopularProducts(
			@RequestParam(required = false) Integer limit) {
		return productService.getPopularProducts(limit);
	}

	@GetMapping("/trending")
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> getTrendingProducts(
			@RequestParam(required = false) Integer limit) {
		return productService.getTrendingProducts(limit);
	}

	@GetMapping("/force-trending")
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> getForceTrendingProducts() {
		return productService.getForceTrendingProducts();
	}

	@GetMapping("/out-of-stock")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<List<ProductResponse>>> getOutOfStockProducts() {
		return productService.getOutOfStockProducts();
	}

	@GetMapping("/low-stock")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<List<ProductStockResponse>>> getLowStockProducts(
			@RequestParam(required = false) Integer threshold) {
		return productService.getLowStockProducts(threshold);
	}

	// ------------------ ADMIN ENDPOINTS ------------------
	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> createProduct(
			@Valid @RequestBody ProductCreateRequest request) {
		return productService.createProduct(request);
	}

	@PutMapping("/{productId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> updateProduct(@PathVariable @NotBlank String productId,
			@Valid @RequestBody ProductUpdateRequest request) {
		return productService.updateProduct(productId, request);
	}

	@PatchMapping("/{productId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> partialUpdateProduct(
			@PathVariable @NotBlank String productId, @RequestBody Map<String, Object> updates) {
		return productService.partialUpdateProduct(productId, updates);
	}

	@DeleteMapping("/{productId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<Void>> deleteProduct(@PathVariable @NotBlank String productId) {
		return productService.deleteProduct(productId);
	}

	@DeleteMapping("/{productId}/permanent")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<Void>> hardDeleteProduct(@PathVariable @NotBlank String productId) {
		return productService.hardDeleteProduct(productId);
	}

	// ------------------ STOCK MANAGEMENT ------------------
	@PutMapping("/{productId}/stock")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> updateStock(@PathVariable @NotBlank String productId,
			@RequestParam @NotNull Integer quantity) {
		return productService.updateStock(productId, quantity);
	}

	@PostMapping("/{productId}/stock/reduce")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> reduceStock(@PathVariable @NotBlank String productId,
			@RequestParam @NotNull Integer quantity) {
		return productService.reduceStock(productId, quantity);
	}

	@PostMapping("/{productId}/stock/add")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> addStock(@PathVariable @NotBlank String productId,
			@RequestParam @NotNull Integer quantity) {
		return productService.addStock(productId, quantity);
	}

	// ------------------ IMAGE MANAGEMENT ------------------
	@PostMapping("/{productId}/images")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> uploadProductImages(
			@PathVariable @NotBlank String productId, @RequestParam("files") MultipartFile[] files,
			@RequestParam(defaultValue = "false") boolean setMainImage) {
		return productService.uploadProductImages(productId, files, setMainImage);
	}

	@DeleteMapping("/{productId}/images")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> removeProductImage(
			@PathVariable @NotBlank String productId, @RequestParam @NotBlank String imageUrl) {
		return productService.removeProductImage(productId, imageUrl);
	}

	@PutMapping("/{productId}/images/main")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> setMainImage(@PathVariable @NotBlank String productId,
			@RequestParam @NotBlank String imageUrl) {
		return productService.setMainImage(productId, imageUrl);
	}

	@PutMapping("/{productId}/images/reorder")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> reorderImages(@PathVariable @NotBlank String productId,
			@RequestBody List<String> imageUrls) {
		return productService.reorderImages(productId, imageUrls);
	}

	// ------------------ ADMIN FEATURES ------------------
	@PutMapping("/admin/{productId}/popular")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> setPopular(@PathVariable @NotBlank String productId,
			@RequestParam @NotNull Boolean value) {
		return productService.setProductPopular(productId, value);
	}

	@PutMapping("/admin/{productId}/force-trending")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductResponse>> setForceTrending(@PathVariable @NotBlank String productId,
			@RequestParam @NotNull Boolean value) {
		return productService.setProductForceTrending(productId, value);
	}

	// ------------------ BULK OPERATIONS ------------------
	@PostMapping("/bulk/availability")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<BulkOperationResult>> bulkUpdateAvailability(
			@RequestBody List<String> productIds, @RequestParam @NotNull Boolean available) {
		return productService.bulkUpdateAvailability(productIds, available);
	}

	@PostMapping("/bulk/prices")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<BulkOperationResult>> bulkUpdatePrices(
			@RequestBody Map<String, BigDecimal> productPriceUpdates) { // Change Double to BigDecimal
		return productService.bulkUpdatePrices(productPriceUpdates);
	}

	@PostMapping("/bulk/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<Void>> deleteProductsBulk(@RequestBody List<String> productIds) {
		return productService.deleteProductsBulk(productIds);
	}

	// ------------------ ANALYTICS ------------------
	@GetMapping("/{productId}/analytics")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ProductAnalyticsResponse>> getProductAnalytics(
			@PathVariable @NotBlank String productId) {
		return productService.getProductAnalytics(productId);
	}

	@GetMapping("/summary")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<List<ProductSummaryResponse>>> getProductsSummary() {
		return productService.getProductsSummary();
	}
}