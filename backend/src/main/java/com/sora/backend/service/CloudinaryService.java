package com.sora.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public CloudinaryUploadResult uploadImage(MultipartFile file, String folder) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("error.file.empty");
        }

        validateImageFile(file);

        Map<String, Object> uploadParams = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
                "quality", "auto",
                "fetch_format", "auto"
        );

        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);

        return new CloudinaryUploadResult(
                (String) uploadResult.get("public_id"),
                (String) uploadResult.get("secure_url"),
                ((Number) uploadResult.get("width")).intValue(),
                ((Number) uploadResult.get("height")).intValue(),
                ((Number) uploadResult.get("bytes")).longValue()
        );
    }

    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    private void validateImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("error.file.invalid_type");
        }

        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("error.file.too_large");
        }
    }

    public record CloudinaryUploadResult(
            String publicId,
            String url,
            Integer width,
            Integer height,
            Long size
    ) {}
}