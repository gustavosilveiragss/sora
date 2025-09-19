package com.sora.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sora.backend.exception.ErrorResponse;
import com.sora.backend.util.MessageUtil;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        String messageKey = "auth.token.required";
        String message = MessageUtil.getMessage(messageKey);
        ErrorResponse errorResponse = new ErrorResponse(messageKey, message);
        
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}