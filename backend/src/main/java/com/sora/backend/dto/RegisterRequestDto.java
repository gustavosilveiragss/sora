package com.sora.backend.dto;

import com.sora.backend.validation.ValidEmail;
import com.sora.backend.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
    @NotBlank(message = "username.required")
    @Size(min = 3, max = 30, message = "username.size")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "username.invalid.characters")
    String username,
    
    @ValidEmail
    @NotBlank(message = "email.required")
    String email,
    
    @ValidPassword
    @NotBlank(message = "password.required")
    String password,
    
    @NotBlank(message = "firstName.required")
    @Size(max = 50, message = "firstName.max.length")
    String firstName,
    
    @NotBlank(message = "lastName.required")
    @Size(max = 50, message = "lastName.max.length")
    String lastName,
    
    @Size(max = 500, message = "bio.max.length")
    String bio
) {}