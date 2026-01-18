package com.printkon.pdp.user;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.user.dto.AddressRequest;
import com.printkon.pdp.user.dto.AddressResponse;
import com.printkon.pdp.user.services.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Slf4j
public class AddressController {

	private final AddressService addressService;

	// === CREATE ENDPOINTS ===

	@PostMapping
	public ResponseEntity<ResponseStructure<AddressResponse>> createAddress(
			@Valid @RequestBody AddressRequest addressRequest) {
		log.info("POST /api/addresses - Creating new address");
		return addressService.createAddress(addressRequest);
	}

	// === READ ENDPOINTS ===

	@GetMapping
	public ResponseEntity<ResponseStructure<List<AddressResponse>>> getAllAddressesForCurrentUser() {
		log.info("GET /api/addresses - Fetching all addresses for current user");
		return addressService.getAllAddressesForCurrentUser();
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<ResponseStructure<List<AddressResponse>>> getAddressesByUserId(@PathVariable Long userId) {
		log.info("GET /api/addresses/user/{} - Fetching addresses for user", userId);
		return addressService.getAddressesByUserId(userId);
	}

	@GetMapping("/{addressId}")
	public ResponseEntity<ResponseStructure<AddressResponse>> getAddressById(@PathVariable Long addressId) {
		log.info("GET /api/addresses/{} - Fetching address by ID", addressId);
		return addressService.getAddressById(addressId);
	}

	@GetMapping("/default")
	public ResponseEntity<ResponseStructure<AddressResponse>> getDefaultAddressForCurrentUser() {
		log.info("GET /api/addresses/default - Fetching default address for current user");
		return addressService.getDefaultAddressForCurrentUser();
	}

	// === UPDATE ENDPOINTS ===

	@PutMapping("/{addressId}")
	public ResponseEntity<ResponseStructure<AddressResponse>> updateAddress(@PathVariable Long addressId,
			@Valid @RequestBody AddressRequest updateRequest) {
		log.info("PUT /api/addresses/{} - Updating address", addressId);
		return addressService.updateAddress(addressId, updateRequest);
	}

	@PatchMapping("/{addressId}/set-default")
	public ResponseEntity<ResponseStructure<AddressResponse>> setDefaultAddress(@PathVariable Long addressId) {
		log.info("PATCH /api/addresses/{}/set-default - Setting address as default", addressId);
		return addressService.setDefaultAddress(addressId);
	}

	// === DELETE ENDPOINTS ===

	@DeleteMapping("/{addressId}")
	public ResponseEntity<ResponseStructure<String>> deleteAddress(@PathVariable Long addressId) {
		log.info("DELETE /api/addresses/{} - Deleting address", addressId);
		return addressService.deleteAddress(addressId);
	}

	@DeleteMapping
	public ResponseEntity<ResponseStructure<String>> deleteAllAddressesForCurrentUser() {
		log.info("DELETE /api/addresses - Deleting all addresses for current user");
		return addressService.deleteAllAddressesForCurrentUser();
	}

	// === SEARCH ENDPOINTS ===

	@GetMapping("/search/city")
	public ResponseEntity<ResponseStructure<List<AddressResponse>>> searchAddressesByCity(@RequestParam String city) {
		log.info("GET /api/addresses/search/city?city={} - Searching addresses by city", city);
		return addressService.searchAddressesByCity(city);
	}

	@GetMapping("/search/state")
	public ResponseEntity<ResponseStructure<List<AddressResponse>>> searchAddressesByState(@RequestParam String state) {
		log.info("GET /api/addresses/search/state?state={} - Searching addresses by state", state);
		return addressService.searchAddressesByState(state);
	}

	@GetMapping("/search/country")
	public ResponseEntity<ResponseStructure<List<AddressResponse>>> searchAddressesByCountry(
			@RequestParam String country) {
		log.info("GET /api/addresses/search/country?country={} - Searching addresses by country", country);
		return addressService.searchAddressesByCountry(country);
	}

	@GetMapping("/search/zip")
	public ResponseEntity<ResponseStructure<List<AddressResponse>>> searchAddressesByZip(@RequestParam String zip) {
		log.info("GET /api/addresses/search/zip?zip={} - Searching addresses by ZIP code", zip);
		return addressService.searchAddressesByZip(zip);
	}

	// === UTILITY ENDPOINTS ===

	@GetMapping("/count")
	public ResponseEntity<ResponseStructure<Long>> getAddressCountForCurrentUser() {
		log.info("GET /api/addresses/count - Getting address count for current user");
		return addressService.getAddressCountForCurrentUser();
	}
}