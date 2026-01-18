package com.printkon.pdp.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.printkon.pdp.storage.config.StorageProperties;
import com.printkon.pdp.storage.dto.ImageUploadResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

	private final StorageProperties storageProperties;

	@Override
	public ImageUploadResponse store(@NotNull MultipartFile file, @NotBlank String category) throws IOException {
		validateFile(file);

		// Create unique filename
		String originalFilename = file.getOriginalFilename();
		String extension = getFileExtension(originalFilename);
		String uniqueFilename = UUID.randomUUID().toString() + extension;

		// Create directory structure
		Path categoryPath = Paths.get(storageProperties.getLocal().getUploadDir(), category);
		Files.createDirectories(categoryPath);

		// Store file
		Path targetLocation = categoryPath.resolve(uniqueFilename);
		Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

		log.info("File stored locally: {}", targetLocation);

		// Return response
		String relativePath = category + "/" + uniqueFilename;
		return ImageUploadResponse.builder().imageUrl(getPublicUrl(relativePath)).fileName(uniqueFilename)
				.category(category).fileSize(file.getSize()).contentType(file.getContentType()).build();
	}

	@Override
	public boolean delete(@NotBlank String fileUrl) {
		try {
			String relativePath = extractRelativePath(fileUrl);
			Path filePath = Paths.get(storageProperties.getLocal().getUploadDir(), relativePath);
			boolean deleted = Files.deleteIfExists(filePath);

			if (deleted) {
				log.info("File deleted locally: {}", filePath);
			} else {
				log.warn("File not found for deletion: {}", filePath);
			}

			return deleted;
		} catch (IOException e) {
			log.error("Error deleting file: {}", fileUrl, e);
			return false;
		}
	}

	@Override
	public String getPublicUrl(@NotBlank String relativePath) {
		return storageProperties.getBaseUrl() + storageProperties.getLocal().getUrlPrefix() + "/" + relativePath;
	}

	private void validateFile(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new IOException("Cannot store empty file");
		}

		String filename = file.getOriginalFilename();
		if (filename == null || filename.contains("..")) {
			throw new IOException("Invalid filename: " + filename);
		}
	}

	private String getFileExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "";
		}
		return filename.substring(filename.lastIndexOf("."));
	}

	private String extractRelativePath(String url) {
		String urlPrefix = storageProperties.getLocal().getUrlPrefix();
		int index = url.indexOf(urlPrefix);
		if (index != -1) {
			return url.substring(index + urlPrefix.length() + 1);
		}
		return url;
	}
}