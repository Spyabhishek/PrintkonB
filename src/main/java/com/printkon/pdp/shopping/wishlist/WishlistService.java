package com.printkon.pdp.shopping.wishlist;

import com.printkon.pdp.catalog.repositories.ProductRepository;
import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.review.repositories.ProductReviewRepository;
import com.printkon.pdp.shopping.wishlist.dto.WishlistItemRequest;
import com.printkon.pdp.shopping.wishlist.dto.WishlistItemResponse;
import com.printkon.pdp.shopping.wishlist.dto.WishlistResponse;
import com.printkon.pdp.shopping.wishlist.models.Wishlist;
import com.printkon.pdp.shopping.wishlist.models.WishlistItem;
import com.printkon.pdp.shopping.wishlist.repositories.WishlistRepository;
import com.printkon.pdp.user.UserDetailsImpl;
import com.printkon.pdp.user.models.User;
import com.printkon.pdp.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

	private final WishlistRepository wishlistRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final ProductReviewRepository productReviewRepository;

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<WishlistResponse>> getWishlist(UserDetailsImpl user) {
		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Wishlist wishlist = wishlistRepository.findByUser(managedUser)
				.orElseGet(() -> Wishlist.builder().user(managedUser).build());

		return buildResponse("Wishlist fetched successfully", mapToResponse(wishlist), HttpStatus.OK);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<WishlistResponse>> addToWishlist(WishlistItemRequest req,
			UserDetailsImpl user) {
		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));
		var product = productRepository.findById(req.getProductId())
				.orElseThrow(() -> new RuntimeException("Product not found"));

		Wishlist wishlist = wishlistRepository.findByUser(managedUser)
				.orElseGet(() -> wishlistRepository.save(Wishlist.builder().user(managedUser).build()));

		boolean exists = wishlist.getItems().stream().anyMatch(i -> i.getProduct().getId().equals(product.getId()));

		if (!exists) {
			WishlistItem item = WishlistItem.builder().product(product).wishlist(wishlist).build();
			wishlist.addItem(item);
			wishlist = wishlistRepository.save(wishlist);
		}

		return buildResponse("Item added to wishlist successfully", mapToResponse(wishlist), HttpStatus.CREATED);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<WishlistResponse>> removeFromWishlist(Long wishlistItemId,
			UserDetailsImpl user) {
		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Wishlist wishlist = wishlistRepository.findByUser(managedUser)
				.orElseThrow(() -> new RuntimeException("Wishlist not found"));

		wishlist.getItems().removeIf(i -> i.getId().equals(wishlistItemId));
		Wishlist saved = wishlistRepository.save(wishlist);

		return buildResponse("Item removed from wishlist successfully", mapToResponse(saved), HttpStatus.OK);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<WishlistResponse>> clearWishlist(UserDetailsImpl user) {
		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Wishlist wishlist = wishlistRepository.findByUser(managedUser)
				.orElseThrow(() -> new RuntimeException("Wishlist not found"));

		wishlist.getItems().clear();
		Wishlist saved = wishlistRepository.save(wishlist);

		return buildResponse("Wishlist cleared successfully", mapToResponse(saved), HttpStatus.OK);
	}

	// ---------------- Helper Methods ----------------

	private ResponseEntity<ResponseStructure<WishlistResponse>> buildResponse(String message, WishlistResponse data,
			HttpStatus status) {
		ResponseStructure<WishlistResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(status.value());
		structure.setMessage(message);
		structure.setData(data);
		return new ResponseEntity<>(structure, status);
	}

	private WishlistResponse mapToResponse(Wishlist wishlist) {
		List<WishlistItemResponse> items = wishlist.getItems().stream().map(i -> {
			var p = i.getProduct();
			Double avgRating = productReviewRepository.findAverageRatingByProductId(p.getId());
			if (avgRating == null)
				avgRating = 0.0;
			Long reviewCount = productReviewRepository.countByProductIdAndApprovedTrue(p.getId());

			return WishlistItemResponse.builder().id(i.getId()).productId(p.getProductId()) // Use productId instead of
																							// id
					.productName(p.getName()).productPrice(p.getPrice()).imageUrl(p.getMainImageUrl()) // Changed from
																										// getImageUrl()
																										// to
																										// getMainImageUrl()
					.available(p.getAvailable()).inStock(p.isInStock()) // Add stock information
					.categoryName(p.getCategory().getName()).averageRating(avgRating).reviewCount(reviewCount).build();
		}).toList();

		return WishlistResponse.builder().wishlistId(wishlist.getId())
				.userId(wishlist.getUser() != null ? wishlist.getUser().getId() : null).items(items).build();
	}
}
