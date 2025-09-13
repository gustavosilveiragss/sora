package com.sora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TravelPermissionRequestDto(
    @NotBlank(message = "grantee.username.required")
    String granteeUsername,
    
    @NotBlank(message = "country.code.required")
    String countryCode,
    
    @Size(max = 500, message = "invitation.message.max.length")
    String invitationMessage
) {}