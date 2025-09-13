package com.sora.backend.config;

import com.sora.backend.service.CloudinaryService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@TestConfiguration
@Profile("test")
public class TestCloudinaryConfig {

    @Bean
    @Primary
    public CloudinaryService testCloudinaryService() {
        return new CloudinaryService(null) {
            @Override
            public CloudinaryUploadResult uploadImage(MultipartFile file, String folder) throws IOException {
                // Simulate successful upload for tests
                return new CloudinaryUploadResult(
                    "test_public_id_" + file.getOriginalFilename(),
                    "https://test-cloudinary.com/" + folder + "/" + file.getOriginalFilename(),
                    800,
                    600,
                    (long) file.getBytes().length
                );
            }

            @Override
            public void deleteImage(String publicId) throws IOException {
                // Simulate successful deletion for tests
                // Do nothing
            }
        };
    }
}