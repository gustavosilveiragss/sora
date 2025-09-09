package com.sora.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequestDto(
    @NotBlank(message = "user.firstname.required")
    @Size(min = 2, max = 50, message = "user.firstname.size")
    String firstName,
    
    @NotBlank(message = "user.lastname.required")
    @Size(min = 2, max = 50, message = "user.lastname.size")
    String lastName,
    
    @Size(max = 500, message = "user.bio.size")
    String bio,
    
    @NotBlank(message = "user.username.required")
    @Size(min = 3, max = 30, message = "user.username.size")
    String username
) {}