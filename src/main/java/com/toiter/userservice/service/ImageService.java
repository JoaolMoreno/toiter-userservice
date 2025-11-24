package com.toiter.userservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${s3.bucket-name}")
    private String bucketName;

    @Value("${s3.public-host:}")
    private String publicHost;

    @Value("${s3.host:}")
    private String s3Host;

    @Value("${s3.presign-duration-days:7}")
    private long presignDurationDays;

    public ImageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public String uploadImage(String existingKey, MultipartFile imageFile) throws IOException {
        String filename = UUID.randomUUID().toString();

        if (existingKey != null && !existingKey.isBlank()) {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(existingKey)
                        .build());
            } catch (Exception ignored) {
                // ignore deletion errors
            }
        }

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucketName)
                .key(filename)
                .build(),
            RequestBody.fromBytes(imageFile.getBytes())
        );

        return filename;
    }

    public String getPublicUrl(String key) {
        if (key == null) return null;
        if (key.startsWith("http://") || key.startsWith("https://")) return key;

        long daysToUse = presignDurationDays;
        if (s3Host != null && s3Host.contains("amazonaws")) {
            daysToUse = Math.min(presignDurationDays, 7);
            if (presignDurationDays > 7) logger.warn("Clamping presign duration from {} to {} for AWS endpoint", presignDurationDays, daysToUse);
        }

        try {
            String url = tryPresign(key, daysToUse);
            logger.info("Presigned URL generated for key {} using {} days", key, daysToUse);
            logger.debug("Presigned URL for key {} (truncated): {}", key, url.length() > 200 ? url.substring(0, 200) + "..." : url);
            return url;
        } catch (Exception e) {
            logger.error("Presign attempt failed for key {} with {} days", key, daysToUse, e);
            String hostToUse = (publicHost != null && !publicHost.isBlank()) ? publicHost : (s3Host != null && !s3Host.isBlank() ? s3Host : null);
            String fallbackUrl = (hostToUse != null && !hostToUse.isBlank())
                    ? String.format("%s/%s/%s", hostToUse.replaceAll("/+$", ""), bucketName, URLEncoder.encode(key, StandardCharsets.UTF_8))
                    : String.format("%s/%s/%s", "https://s3.amazonaws.com", bucketName, URLEncoder.encode(key, StandardCharsets.UTF_8));
            logger.info("Returning fallback URL for key {}", key);
            logger.debug("Fallback URL for key {}: {}", key, fallbackUrl);
            return fallbackUrl;
        }
    }

    private String tryPresign(String key, long days) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofDays(days))
                .build();

        var presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url().toString();
    }
}
