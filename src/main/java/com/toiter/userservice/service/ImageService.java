package com.toiter.userservice.service;

import com.toiter.userservice.entity.Image;
import com.toiter.userservice.repository.ImageRepository;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class ImageService {

    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image getImageById(@NotNull @Min(1) Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
    }

    public Image updateOrCreateImage(@NotNull @Min(1) Long imageId, MultipartFile imageFile) throws IOException {
        byte[] imageBytes = imageFile.getBytes();

        Image image = imageId != null
                ? imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"))
                : new Image();

        image.setImage(imageBytes);

        if (imageId == null) {
            image.setCreationDate(LocalDateTime.now());
        }

        return imageRepository.save(image);
    }
}
