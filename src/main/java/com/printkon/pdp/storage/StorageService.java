package com.printkon.pdp.storage;

import org.springframework.web.multipart.MultipartFile;

import com.printkon.pdp.storage.dto.ImageUploadResponse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;

public interface StorageService {
	ImageUploadResponse store(@NotNull MultipartFile file, @NotBlank String category) throws IOException;

	boolean delete(@NotBlank String fileUrl);

	String getPublicUrl(@NotBlank String fileName);
}