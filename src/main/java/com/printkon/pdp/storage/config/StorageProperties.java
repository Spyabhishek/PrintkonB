// src/main/java/com/printlok/pdp/config/StorageProperties.java
package com.printkon.pdp.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
@Component
@ConfigurationProperties(prefix = "app.storage")
@Validated
public class StorageProperties {

	@NotBlank(message = "Storage type is required")
	@Pattern(regexp = "^(local|s3)$", message = "Storage type must be either 'local' or 's3'")
	private String type = "local";

	@NotBlank(message = "Base URL is required")
	private String baseUrl;

	@Valid
	private Local local = new Local();

	@Valid
	private S3 s3 = new S3();

	@Data
	public static class Local {
		@NotBlank(message = "Upload directory is required")
		private String uploadDir = "uploads/images";

		@NotBlank(message = "URL prefix is required")
		private String urlPrefix = "/api/images";
	}

	@Data
	public static class S3 {
		private String bucketName;
		private String region;
		private String accessKey;
		private String secretKey;

		@NotBlank(message = "Folder prefix is required")
		private String folderPrefix = "images";
	}
}