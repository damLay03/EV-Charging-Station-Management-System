package com.swp.evchargingstation.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryService {

    Cloudinary cloudinary;

    /**
     * Upload vehicle document image to Cloudinary
     * @param file MultipartFile from driver
     * @return Cloudinary secure URL
     */
    public String uploadVehicleDocument(MultipartFile file) {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_FILE);
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_FILE_TYPE);
        }

        // Validate file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        try {
            log.info("Uploading vehicle document to Cloudinary, filename: {}, size: {} bytes",
                    file.getOriginalFilename(), file.getSize());

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "vehicle-documents",
                            "resource_type", "image",
                            "format", "jpg",
                            "quality", "auto:good",
                            "transformation", new com.cloudinary.Transformation()
                                    .width(1200)
                                    .height(1200)
                                    .crop("limit")
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Vehicle document uploaded successfully to Cloudinary: {}", secureUrl);

            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload vehicle document to Cloudinary", e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    /**
     * Delete image from Cloudinary by URL
     * @param imageUrl Cloudinary URL
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            // Extract public_id from URL
            String publicId = extractPublicIdFromUrl(imageUrl);

            if (publicId != null) {
                log.info("Deleting image from Cloudinary, publicId: {}", publicId);
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Image deleted successfully from Cloudinary");
            }
        } catch (Exception e) {
            log.error("Failed to delete image from Cloudinary: {}", imageUrl, e);
            // Don't throw exception, just log the error
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     * Example: https://res.cloudinary.com/dppc2tng8/image/upload/v1234567890/vehicle-documents/abc123.jpg
     * Returns: vehicle-documents/abc123
     */
    private String extractPublicIdFromUrl(String url) {
        try {
            if (url == null || !url.contains("cloudinary.com")) {
                return null;
            }

            // Find the position of "upload/" in the URL
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }

            // Get the part after "upload/v1234567890/"
            String afterUpload = url.substring(uploadIndex + "/upload/".length());

            // Remove version number (e.g., "v1234567890/")
            int slashIndex = afterUpload.indexOf('/');
            if (slashIndex == -1) {
                return null;
            }

            String pathWithExtension = afterUpload.substring(slashIndex + 1);

            // Remove file extension
            int lastDotIndex = pathWithExtension.lastIndexOf('.');
            if (lastDotIndex != -1) {
                return pathWithExtension.substring(0, lastDotIndex);
            }

            return pathWithExtension;
        } catch (Exception e) {
            log.error("Failed to extract public_id from URL: {}", url, e);
            return null;
        }
    }
}

