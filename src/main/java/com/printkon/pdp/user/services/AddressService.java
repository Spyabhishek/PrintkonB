package com.printkon.pdp.user.services;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.exceptions.AddressNotFoundException;
import com.printkon.pdp.exceptions.UnauthorizedException;
import com.printkon.pdp.exceptions.UserNotFoundException;
import com.printkon.pdp.user.dto.AddressRequest;
import com.printkon.pdp.user.dto.AddressResponse;
import com.printkon.pdp.user.models.Address;
import com.printkon.pdp.user.models.User;
import com.printkon.pdp.user.repositories.AddressRepository;
import com.printkon.pdp.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

	private final AddressRepository addressRepository;
	private final UserRepository userRepository;

	// === CREATE OPERATIONS ===

	@Transactional
	public ResponseEntity<ResponseStructure<AddressResponse>> createAddress(AddressRequest addressRequest) {
		log.info("Creating new address for user");

		User currentUser = getCurrentAuthenticatedUser();

		// If this address is set as default, clear existing default addresses
		if (addressRequest.getIsDefault()) {
			log.debug("Clearing existing default addresses for user ID: {}", currentUser.getId());
			addressRepository.clearDefaultAddresses(currentUser.getId());
		}

		Address address = mapToAddress(addressRequest);
		address.setUser(currentUser);

		Address savedAddress = addressRepository.save(address);
		log.info("Address created successfully with ID: {}", savedAddress.getId());

		ResponseStructure<AddressResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.CREATED.value());
		structure.setMessage("Address created successfully");
		structure.setData(mapToAddressResponse(savedAddress));

		return ResponseEntity.status(HttpStatus.CREATED).body(structure);
	}

	// === READ OPERATIONS ===

	public ResponseEntity<ResponseStructure<List<AddressResponse>>> getAllAddressesForCurrentUser() {
		User currentUser = getCurrentAuthenticatedUser();
		log.debug("Fetching all addresses for user ID: {}", currentUser.getId());

		List<Address> addresses = addressRepository.findByUserId(currentUser.getId());

		ResponseStructure<List<AddressResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (addresses.isEmpty()) {
			structure.setMessage("No addresses found");
			structure.setData(List.of());
			log.debug("No addresses found for user ID: {}", currentUser.getId());
		} else {
			List<AddressResponse> addressResponses = addresses.stream().map(this::mapToAddressResponse)
					.collect(Collectors.toList());
			structure.setMessage("Addresses fetched successfully");
			structure.setData(addressResponses);
			log.debug("Found {} addresses for user ID: {}", addresses.size(), currentUser.getId());
		}

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<List<AddressResponse>>> getAddressesByUserId(Long userId) {
		log.debug("Fetching addresses for user ID: {}", userId);

		// Only allow admins or the user themselves to access addresses
		User currentUser = getCurrentAuthenticatedUser();
		if (!currentUser.getId().equals(userId) && !isAdmin()) {
			log.warn("Unauthorized access attempt to addresses of user ID: {} by user ID: {}", userId,
					currentUser.getId());
			throw new UnauthorizedException("You are not authorized to access these addresses");
		}

		List<Address> addresses = addressRepository.findByUserId(userId);

		ResponseStructure<List<AddressResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (addresses.isEmpty()) {
			structure.setMessage("No addresses found for user ID: " + userId);
			structure.setData(List.of());
		} else {
			List<AddressResponse> addressResponses = addresses.stream().map(this::mapToAddressResponse)
					.collect(Collectors.toList());
			structure.setMessage("Addresses fetched successfully");
			structure.setData(addressResponses);
		}

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<AddressResponse>> getAddressById(Long addressId) {
		log.debug("Fetching address by ID: {}", addressId);

		Address address = getAddressByIdAndCheckOwnership(addressId);

		ResponseStructure<AddressResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Address fetched successfully");
		structure.setData(mapToAddressResponse(address));

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<AddressResponse>> getDefaultAddressForCurrentUser() {
		User currentUser = getCurrentAuthenticatedUser();
		log.debug("Fetching default address for user ID: {}", currentUser.getId());

		List<Address> defaultAddresses = addressRepository.findByUserIdAndIsDefaultTrue(currentUser.getId());

		ResponseStructure<AddressResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (defaultAddresses.isEmpty()) {
			structure.setMessage("No default address found");
			structure.setData(null);
			log.debug("No default address found for user ID: {}", currentUser.getId());
		} else {
			structure.setMessage("Default address fetched successfully");
			structure.setData(mapToAddressResponse(defaultAddresses.get(0)));
			log.debug("Default address found with ID: {}", defaultAddresses.get(0).getId());
		}

		return ResponseEntity.ok(structure);
	}

	// === UPDATE OPERATIONS ===

	@Transactional
	public ResponseEntity<ResponseStructure<AddressResponse>> updateAddress(Long addressId,
			AddressRequest updateRequest) {
		log.info("Updating address with ID: {}", addressId);

		Address address = getAddressByIdAndCheckOwnership(addressId);

		// Update all fields
		address.setLabel(updateRequest.getLabel());
		address.setRecipientName(updateRequest.getRecipientName());
		address.setPhone(updateRequest.getPhone());
		address.setAddressLine(updateRequest.getAddressLine());
		address.setCity(updateRequest.getCity());
		address.setState(updateRequest.getState());
		address.setZip(updateRequest.getZip());
		address.setCountry(updateRequest.getCountry());

		// Handle default address logic
		if (updateRequest.getIsDefault()) {
			log.debug("Setting address as default, clearing other defaults for user ID: {}", address.getUser().getId());
			addressRepository.clearDefaultAddresses(address.getUser().getId());
			address.setDefault(true);
		} else {
			address.setDefault(false);
		}

		Address updatedAddress = addressRepository.save(address);
		log.info("Address updated successfully with ID: {}", addressId);

		ResponseStructure<AddressResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Address updated successfully");
		structure.setData(mapToAddressResponse(updatedAddress));

		return ResponseEntity.ok(structure);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<AddressResponse>> setDefaultAddress(Long addressId) {
		log.info("Setting address as default with ID: {}", addressId);

		Address address = getAddressByIdAndCheckOwnership(addressId);

		// Clear existing default addresses
		addressRepository.clearDefaultAddresses(address.getUser().getId());

		// Set this address as default
		address.setDefault(true);
		Address updatedAddress = addressRepository.save(address);
		log.info("Address set as default successfully with ID: {}", addressId);

		ResponseStructure<AddressResponse> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Address set as default successfully");
		structure.setData(mapToAddressResponse(updatedAddress));

		return ResponseEntity.ok(structure);
	}

	// === DELETE OPERATIONS ===

	@Transactional
	public ResponseEntity<ResponseStructure<String>> deleteAddress(Long addressId) {
		log.info("Deleting address with ID: {}", addressId);

		Address address = getAddressByIdAndCheckOwnership(addressId);

		addressRepository.delete(address);
		log.info("Address deleted successfully with ID: {}", addressId);

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Address deleted successfully");
		structure.setData("Address with ID " + addressId + " has been deleted");

		return ResponseEntity.ok(structure);
	}

	@Transactional
	public ResponseEntity<ResponseStructure<String>> deleteAllAddressesForCurrentUser() {
		User currentUser = getCurrentAuthenticatedUser();
		log.info("Deleting all addresses for user ID: {}", currentUser.getId());

		addressRepository.deleteAllByUserId(currentUser.getId());
		log.info("All addresses deleted for user ID: {}", currentUser.getId());

		ResponseStructure<String> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("All addresses deleted successfully");
		structure.setData("All addresses for user ID " + currentUser.getId() + " have been deleted");

		return ResponseEntity.ok(structure);
	}

	// === SEARCH AND FILTER OPERATIONS ===

	public ResponseEntity<ResponseStructure<List<AddressResponse>>> searchAddressesByCity(String city) {
		User currentUser = getCurrentAuthenticatedUser();
		log.debug("Searching addresses by city: {} for user ID: {}", city, currentUser.getId());

		List<Address> addresses = addressRepository.findByCityContainingIgnoreCase(city).stream()
				.filter(address -> address.getUser().getId().equals(currentUser.getId())).collect(Collectors.toList());

		ResponseStructure<List<AddressResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (addresses.isEmpty()) {
			structure.setMessage("No addresses found in city: " + city);
			structure.setData(List.of());
		} else {
			List<AddressResponse> addressResponses = addresses.stream().map(this::mapToAddressResponse)
					.collect(Collectors.toList());
			structure.setMessage("Addresses found successfully");
			structure.setData(addressResponses);
		}

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<List<AddressResponse>>> searchAddressesByState(String state) {
		User currentUser = getCurrentAuthenticatedUser();
		log.debug("Searching addresses by state: {} for user ID: {}", state, currentUser.getId());

		List<Address> addresses = addressRepository.findByStateContainingIgnoreCase(state).stream()
				.filter(address -> address.getUser().getId().equals(currentUser.getId())).collect(Collectors.toList());

		ResponseStructure<List<AddressResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (addresses.isEmpty()) {
			structure.setMessage("No addresses found in state: " + state);
			structure.setData(List.of());
		} else {
			List<AddressResponse> addressResponses = addresses.stream().map(this::mapToAddressResponse)
					.collect(Collectors.toList());
			structure.setMessage("Addresses found successfully");
			structure.setData(addressResponses);
		}

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<List<AddressResponse>>> searchAddressesByCountry(String country) {
		User currentUser = getCurrentAuthenticatedUser();
		log.debug("Searching addresses by country: {} for user ID: {}", country, currentUser.getId());

		List<Address> addresses = addressRepository.findByCountryContainingIgnoreCase(country).stream()
				.filter(address -> address.getUser().getId().equals(currentUser.getId())).collect(Collectors.toList());

		ResponseStructure<List<AddressResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (addresses.isEmpty()) {
			structure.setMessage("No addresses found in country: " + country);
			structure.setData(List.of());
		} else {
			List<AddressResponse> addressResponses = addresses.stream().map(this::mapToAddressResponse)
					.collect(Collectors.toList());
			structure.setMessage("Addresses found successfully");
			structure.setData(addressResponses);
		}

		return ResponseEntity.ok(structure);
	}

	public ResponseEntity<ResponseStructure<List<AddressResponse>>> searchAddressesByZip(String zip) {
		User currentUser = getCurrentAuthenticatedUser();
		log.debug("Searching addresses by ZIP: {} for user ID: {}", zip, currentUser.getId());

		List<Address> addresses = addressRepository.findByZip(zip).stream()
				.filter(address -> address.getUser().getId().equals(currentUser.getId())).collect(Collectors.toList());

		ResponseStructure<List<AddressResponse>> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());

		if (addresses.isEmpty()) {
			structure.setMessage("No addresses found with ZIP code: " + zip);
			structure.setData(List.of());
		} else {
			List<AddressResponse> addressResponses = addresses.stream().map(this::mapToAddressResponse)
					.collect(Collectors.toList());
			structure.setMessage("Addresses found successfully");
			structure.setData(addressResponses);
		}

		return ResponseEntity.ok(structure);
	}

	// === UTILITY METHODS ===

	public ResponseEntity<ResponseStructure<Long>> getAddressCountForCurrentUser() {
		User currentUser = getCurrentAuthenticatedUser();
		long count = addressRepository.countByUserId(currentUser.getId());

		log.debug("Address count for user ID {}: {}", currentUser.getId(), count);

		ResponseStructure<Long> structure = new ResponseStructure<>();
		structure.setStatusCode(HttpStatus.OK.value());
		structure.setMessage("Address count retrieved successfully");
		structure.setData(count);

		return ResponseEntity.ok(structure);
	}

	public boolean addressBelongsToUser(Long addressId, Long userId) {
		return addressRepository.existsByIdAndUserId(addressId, userId);
	}

	// === HELPER METHODS ===

	private User getCurrentAuthenticatedUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()
				|| authentication.getPrincipal().equals("anonymousUser")) {
			throw new UnauthorizedException("User is not authenticated");
		}

		String email = authentication.getName();
		return userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));
	}

	private Address getAddressByIdAndCheckOwnership(Long addressId) {
		User currentUser = getCurrentAuthenticatedUser();

		return addressRepository.findByIdAndUserId(addressId, currentUser.getId()).orElseThrow(
				() -> new AddressNotFoundException("Address not found with ID: " + addressId + " for current user"));
	}

	private boolean isAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication.getAuthorities().stream()
				.anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
	}

	private Address mapToAddress(AddressRequest addressRequest) {
		return Address.builder().label(addressRequest.getLabel()).recipientName(addressRequest.getRecipientName())
				.phone(addressRequest.getPhone()).addressLine(addressRequest.getAddressLine())
				.city(addressRequest.getCity()).state(addressRequest.getState()).zip(addressRequest.getZip())
				.country(addressRequest.getCountry()).isDefault(addressRequest.getIsDefault()).build();
	}

	private AddressResponse mapToAddressResponse(Address address) {
		return AddressResponse.builder().id(address.getId()).label(address.getLabel())
				.recipientName(address.getRecipientName()).phone(address.getPhone())
				.addressLine(address.getAddressLine()).city(address.getCity()).state(address.getState())
				.zip(address.getZip()).country(address.getCountry()).isDefault(address.isDefault())
				.userId(address.getUser().getId()).build();
	}
}