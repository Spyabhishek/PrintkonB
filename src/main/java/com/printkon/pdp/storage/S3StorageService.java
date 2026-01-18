package com.printkon.pdp.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import com.printkon.pdp.storage.config.StorageProperties;
import com.printkon.pdp.storage.dto.ImageUploadResponse;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Validated
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {
    
    private final StorageProperties storageProperties;
    private S3Client s3Client;
    
    @PostConstruct
    public void init() {
        StorageProperties.S3 s3Config = storageProperties.getS3();
        
        if (s3Config.getAccessKey() == null || s3Config.getSecretKey() == null) {
            throw new IllegalArgumentException("AWS credentials are required for S3 storage");
        }
        
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
            s3Config.getAccessKey(), 
            s3Config.getSecretKey()
        );
        
        this.s3Client = S3Client.builder()
            .region(Region.of(s3Config.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
            .build();
            
        log.info("S3 Storage Service initialized for bucket: {}", s3Config.getBucketName());
    }
    
    @Override
    public ImageUploadResponse store(@NotNull MultipartFile file, @NotBlank String category) throws IOException {
        validateFile(file);
        
        // Create unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + extension;
        
        // Create S3 key
        String s3Key = storageProperties.getS3().getFolderPrefix() + "/" + category + "/" + uniqueFilename;
        
        try {
            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(storageProperties.getS3().getBucketName())
                .key(s3Key)
                .contentType(file.getContentType())
                .build();
                
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("File uploaded to S3: {}", s3Key);
            
            // Return response
            return ImageUploadResponse.builder()
                    .imageUrl(getPublicUrl(s3Key))
                    .fileName(uniqueFilename)
                    .category(category)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();
                    
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", s3Key, e);
            throw new IOException("Failed to upload file to S3: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean delete(@NotBlank String fileUrl) {
        try {
            String s3Key = extractS3Key(fileUrl);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(storageProperties.getS3().getBucketName())
                .key(s3Key)
                .build();
                
            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted from S3: {}", s3Key);
            return true;
            
        } catch (Exception e) {
            log.error("Error deleting file from S3: {}", fileUrl, e);
            return false;
        }
    }
    
    @Override
    public String getPublicUrl(@NotBlank String s3Key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
            storageProperties.getS3().getBucketName(),
            storageProperties.getS3().getRegion(),
            s3Key);
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
    
    private String extractS3Key(String url) {
        // Extract S3 key from URL
        String[] parts = url.split(".amazonaws.com/");
        return parts.length > 1 ? parts[1] : url;
    }
}