package com.sora.backend.dto;

import com.sora.backend.model.PostSharingOption;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequestDto(
    @NotBlank(message = "country.code.required")
    String countryCode,
    
    @NotBlank(message = "collection.code.required")
    String collectionCode,
    
    @NotBlank(message = "city.name.required")
    @Size(max = 100, message = "city.name.max.length")
    String cityName,
    
    @NotNull(message = "city.latitude.required")
    Double cityLatitude,
    
    @NotNull(message = "city.longitude.required")
    Double cityLongitude,
    
    @Size(max = 1000, message = "caption.max.length")
    String caption,
    
    PostSharingOption collaborationOption,
    
    Long collaboratorUserId,
    
    String sharingOption
) {}