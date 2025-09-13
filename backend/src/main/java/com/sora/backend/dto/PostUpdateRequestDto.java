package com.sora.backend.dto;

import jakarta.validation.constraints.Size;

public record PostUpdateRequestDto(
    @Size(max = 1000, message = "caption.max.length")
    String caption,
    
    String collectionCode
) {}