package com.toiter.postservice.model;

import java.time.LocalDateTime;

public record Post(
        Long id,
        Long parentPostId,
        Long repostParentId,
        Long userId,
        String content,
        String mediaUrl,
        LocalDateTime createdAt,
        LocalDateTime deletedAt,
        boolean deleted
) {
}