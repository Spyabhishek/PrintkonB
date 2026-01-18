package com.printkon.pdp.shopping.cart;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.shopping.cart.dto.CartItemRequest;
import com.printkon.pdp.shopping.cart.dto.CartResponse;
import com.printkon.pdp.user.UserDetailsImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@GetMapping
	public ResponseEntity<ResponseStructure<CartResponse>> getCart(@AuthenticationPrincipal UserDetailsImpl user) {
		return cartService.getCartForUser(user);
	}

	@PostMapping
	public ResponseEntity<ResponseStructure<CartResponse>> addItem(@Valid @RequestBody CartItemRequest request,
			@AuthenticationPrincipal UserDetailsImpl user) {
		return cartService.addToCart(request, user);
	}

	@PutMapping("/item/{cartItemId}")
	public ResponseEntity<ResponseStructure<CartResponse>> updateItemQuantity(
			@PathVariable @NotNull @Positive Long cartItemId, @RequestParam @NotNull @Positive Integer quantity,
			@AuthenticationPrincipal UserDetailsImpl user) {
		return cartService.updateQuantity(cartItemId, quantity, user);
	}

	@DeleteMapping("/item/{cartItemId}")
	public ResponseEntity<ResponseStructure<CartResponse>> removeItem(@PathVariable @NotNull @Positive Long cartItemId,
			@AuthenticationPrincipal UserDetailsImpl user) {
		return cartService.removeItem(cartItemId, user);
	}

	@DeleteMapping("/clear")
	public ResponseEntity<ResponseStructure<CartResponse>> clearCart(@AuthenticationPrincipal UserDetailsImpl user) {
		return cartService.clearCart(user);
	}
}