package com.toiter.userservice.controller;

import com.toiter.userservice.entity.Image;
import com.toiter.userservice.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/")
@Tag(name = "Image Controller", description = "APIs para gerenciar imagens")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/images/{id}")
    @Operation(
            summary = "Obter imagem por ID",
            description = "Retorna uma imagem com base no ID fornecido. Se a imagem não for encontrada, retorna uma resposta HTTP 404.",
            security = {@SecurityRequirement(name = "bearerAuth")},
            responses = {
                    @ApiResponse(responseCode = "200", description = "Imagem retornada com sucesso",
                            content = @Content(mediaType = "image/jpeg")),
                    @ApiResponse(responseCode = "404", description = "Imagem não encontrada")
            }
    )
    public ResponseEntity<byte[]> getImageById(
            @PathVariable @NotNull @Min(0) Long id) {
        Image image = imageService.getImageById(id);

        if (image == null || image.getImage() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(image.getImage());
    }
}
