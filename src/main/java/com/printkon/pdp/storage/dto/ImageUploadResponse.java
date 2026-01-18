package com.printkon.pdp.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageUploadResponse {
	private String imageUrl;
	private String fileName;
	private String category;
	private Long fileSize;
	private String contentType;
}