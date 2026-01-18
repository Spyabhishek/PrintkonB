package com.printkon.pdp.shopping.cart;

import com.printkon.pdp.catalog.models.Product;
import com.printkon.pdp.catalog.repositories.ProductRepository;
import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.shopping.cart.dto.CartItemRequest;
import com.printkon.pdp.shopping.cart.dto.CartItemResponse;
import com.printkon.pdp.shopping.cart.dto.CartResponse;
import com.printkon.pdp.shopping.cart.models.Cart;
import com.printkon.pdp.shopping.cart.models.CartItem;
import com.printkon.pdp.shopping.cart.repositories.CartRepository;
import com.printkon.pdp.user.UserDetailsImpl;
import com.printkon.pdp.user.models.User;
import com.printkon.pdp.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartService {

	private final CartRepository cartRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public ResponseEntity<ResponseStructure<CartResponse>> getCartForUser(UserDetailsImpl user) {
		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Cart cart = cartRepository.findByUser(managedUser).orElseGet(() -> Cart.builder().user(managedUser).build());

		return buildResponse("Cart fetched successfully", mapToResponse(cart), HttpStatus.OK);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<CartResponse>> addToCart(CartItemRequest req, UserDetailsImpl user) {
		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		// FIX: Use findByProductId instead of findById
		Product product = productRepository.findByProductId(req.getProductId())
				.orElseThrow(() -> new RuntimeException("Product not found with ID: " + req.getProductId()));

		Cart cart = cartRepository.findByUser(managedUser)
				.orElseGet(() -> cartRepository.save(Cart.builder().user(managedUser).build()));

		// FIX: Simplify the stream logic
		boolean itemExists = cart.getItems().stream()
				.anyMatch(i -> i.getProduct().getProductId().equals(product.getProductId())
						&& Objects.equals(i.getOptions(), req.getOptions()));

		if (itemExists) {
			// Update existing item quantity
			cart.getItems().stream()
					.filter(i -> i.getProduct().getProductId().equals(product.getProductId())
							&& Objects.equals(i.getOptions(), req.getOptions()))
					.findFirst().ifPresent(
							i -> i.setQuantity(i.getQuantity() + (req.getQuantity() == null ? 1 : req.getQuantity())));
		} else {
			// Add new item
			CartItem newItem = CartItem.builder().product(product)
					.quantity(req.getQuantity() == null ? 1 : req.getQuantity()).options(req.getOptions()).cart(cart)
					.build();
			cart.addItem(newItem);
		}

		Cart saved = cartRepository.save(cart);
		return buildResponse("Item added to cart successfully", mapToResponse(saved), HttpStatus.CREATED);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<CartResponse>> updateQuantity(Long cartItemId, Integer quantity,
			UserDetailsImpl user) {
		if (quantity == null || quantity <= 0) {
			throw new IllegalArgumentException("Quantity must be >= 1");
		}

		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Cart cart = cartRepository.findByUser(managedUser).orElseThrow(() -> new RuntimeException("Cart not found"));

		CartItem item = cart.getItems().stream().filter(i -> i.getId().equals(cartItemId)).findFirst()
				.orElseThrow(() -> new RuntimeException("Cart item not found"));

		item.setQuantity(quantity);

		Cart saved = cartRepository.save(cart);
		return buildResponse("Cart item updated successfully", mapToResponse(saved), HttpStatus.OK);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<CartResponse>> removeItem(Long cartItemId, UserDetailsImpl user) {
		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Cart cart = cartRepository.findByUser(managedUser).orElseThrow(() -> new RuntimeException("Cart not found"));

		cart.getItems().removeIf(i -> i.getId().equals(cartItemId));

		Cart saved = cartRepository.save(cart);
		return buildResponse("Item removed from cart successfully", mapToResponse(saved), HttpStatus.OK);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<CartResponse>> clearCart(UserDetailsImpl user) {
		User managedUser = userRepository.findById(user.getId())
				.orElseThrow(() -> new RuntimeException("User not found"));

		Cart cart = cartRepository.findByUser(managedUser).orElseThrow(() -> new RuntimeException("Cart not found"));

		cart.getItems().clear();

		Cart saved = cartRepository.save(cart);
		return buildResponse("Cart cleared successfully", mapToResponse(saved), HttpStatus.OK);
	}

	// ---------------- Helper Methods ----------------

	private ResponseEntity<ResponseStructure<CartResponse>> buildResponse(String message, CartResponse data,
			HttpStatus status) {
		ResponseStructure<CartResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(status.value());
		structure.setMessage(message);
		structure.setData(data);
		return new ResponseEntity<>(structure, status);
	}

	private CartResponse mapToResponse(Cart cart) {
		List<CartItemResponse> items = cart.getItems().stream().map(this::mapToCartItemResponse).toList();

		BigDecimal total = calculateTotalAmount(items);

		return CartResponse.builder().cartId(cart.getId())
				.userId(cart.getUser() != null ? cart.getUser().getId() : null).items(items).totalAmount(total).build();
	}

	private CartItemResponse mapToCartItemResponse(CartItem cartItem) {
		Product product = cartItem.getProduct();
		return CartItemResponse.builder().id(cartItem.getId()).productId(product.getProductId())
				.productName(product.getName()).productPrice(product.getPrice()).quantity(cartItem.getQuantity())
				.options(cartItem.getOptions()).imageUrl(product.getMainImageUrl()).available(product.getAvailable())
				.inStock(product.isInStock()).build();
	}

	private BigDecimal calculateTotalAmount(List<CartItemResponse> items) {
		return items.stream().map(item -> item.getProductPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}