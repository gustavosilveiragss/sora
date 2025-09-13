package com.sora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequestDto(
    @NotBlank(message = "validation.comment.content.required")
    @Size(max = 1000, message = "validation.comment.content.size")
    String content
) {}