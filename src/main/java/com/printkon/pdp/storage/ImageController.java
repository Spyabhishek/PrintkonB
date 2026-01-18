package com.printkon.pdp.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.printkon.pdp.common.dto.ResponseStructure;
import com.printkon.pdp.storage.config.StorageProperties;
import com.printkon.pdp.storage.dto.ImageUploadResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Validated
@CrossOrigin("*")
public class ImageController {

	private final StorageService storageService;
	private final StorageProperties storageProperties;

	private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png",
			"image/gif", "image/webp");

	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

	@PostMapping("/upload")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<ImageUploadResponse>> uploadImage(
			@RequestParam("file") @NotNull MultipartFile file,
			@RequestParam(value = "category", defaultValue = "products") @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Category must contain only alphanumeric characters, hyphens, and underscores") String category) {

		log.info("Uploading image to category: {}", category);

		try {
			// Validate file
			validateFile(file);

			// Store file
			ImageUploadResponse uploadResponse = storageService.store(file, category);

			ResponseStructure<ImageUploadResponse> response = ResponseStructure.<ImageUploadResponse>builder()
					.statusCode(200).message("Image uploaded successfully").data(uploadResponse).build();

			log.info("Image uploaded successfully: {}", uploadResponse.getImageUrl());
			return ResponseEntity.ok(response);

		} catch (IOException | IllegalArgumentException e) {
			log.error("Failed to upload image", e);
			ResponseStructure<ImageUploadResponse> response = ResponseStructure.<ImageUploadResponse>builder()
					.statusCode(400).message("Upload failed: " + e.getMessage()).data(null).build();

			return ResponseEntity.badRequest().body(response);
		}
	}

	@DeleteMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ResponseStructure<Boolean>> deleteImage(@RequestParam("url") @NotBlank String imageUrl) {

		log.info("Deleting image: {}", imageUrl);
		boolean deleted = storageService.delete(imageUrl);

		ResponseStructure<Boolean> response = ResponseStructure.<Boolean>builder().statusCode(deleted ? 200 : 400)
				.message(deleted ? "Image deleted successfully" : "Failed to delete image").data(deleted).build();

		return ResponseEntity.ok(response);
	}

	// This endpoint is only used for local storage to serve images
	@GetMapping("/{category}/{filename}")
	public ResponseEntity<Resource> serveImage(@PathVariable @NotBlank String category,
			@PathVariable @NotBlank String filename) {

		// Only serve images if using local storage
		if (!"local".equals(storageProperties.getType())) {
			return ResponseEntity.notFound().build();
		}

		try {
			Path filePath = Paths.get(storageProperties.getLocal().getUploadDir(), category, filename);
			Resource resource = new FileSystemResource(filePath);

			if (!resource.exists()) {
				log.warn("Image not found: {}", filePath);
				return ResponseEntity.notFound().build();
			}

			String contentType = Files.probeContentType(filePath);
			if (contentType == null) {
				contentType = "application/octet-stream";
			}

			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CACHE_CONTROL, "max-age=3600").body(resource);

		} catch (IOException e) {
			log.error("Error serving image: {}/{}", category, filename, e);
			return ResponseEntity.internalServerError().build();
		}
	}

	private void validateFile(MultipartFile file) throws IllegalArgumentException {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("File is empty");
		}

		if (file.getSize() > MAX_FILE_SIZE) {
			throw new IllegalArgumentException("File size exceeds 5MB limit");
		}

		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
			throw new IllegalArgumentException("Invalid file type. Only JPEG, PNG, GIF, and WebP are allowed");
		}

		String filename = file.getOriginalFilename();
		if (filename == null || filename.contains("..")) {
			throw new IllegalArgumentException("Invalid filename");
		}
	}
}