package com.toiter.userservice.service;

import com.toiter.userservice.entity.Image;
import com.toiter.userservice.repository.ImageRepository;
import org.springframework.stereotype.Service;

@Service
public class ImageService {

    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image getImageById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
    }
}
