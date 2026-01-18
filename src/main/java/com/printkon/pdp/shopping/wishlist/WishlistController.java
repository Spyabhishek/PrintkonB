package com.printkon.pdp.shopping.wishlist;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.shopping.wishlist.dto.WishlistItemRequest;
import com.printkon.pdp.shopping.wishlist.dto.WishlistResponse;
import com.printkon.pdp.user.UserDetailsImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

	private final WishlistService wishlistService;

	@GetMapping
	public ResponseEntity<ResponseStructure<WishlistResponse>> getWishlist(
			@AuthenticationPrincipal UserDetailsImpl user) {
		return wishlistService.getWishlist(user);
	}

	@PostMapping
	public ResponseEntity<ResponseStructure<WishlistResponse>> addItem(@Valid @RequestBody WishlistItemRequest request,
			@AuthenticationPrincipal UserDetailsImpl user) {
		return wishlistService.addToWishlist(request, user);
	}

	@DeleteMapping("/item/{wishlistItemId}")
	public ResponseEntity<ResponseStructure<WishlistResponse>> removeItem(
			@PathVariable @NotNull @Positive Long wishlistItemId, @AuthenticationPrincipal UserDetailsImpl user) {
		return wishlistService.removeFromWishlist(wishlistItemId, user);
	}

	@DeleteMapping("/clear")
	public ResponseEntity<ResponseStructure<WishlistResponse>> clearWishlist(
			@AuthenticationPrincipal UserDetailsImpl user) {
		return wishlistService.clearWishlist(user);
	}
}