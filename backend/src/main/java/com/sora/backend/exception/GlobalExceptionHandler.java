package com.sora.backend.exception;

import com.sora.backend.util.MessageUtil;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        String messageKey = "error.entity.not_found";
        String message = MessageUtil.getMessage(messageKey);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String messageKey = "error.validation.failed";
        String message = MessageUtil.getMessage(messageKey);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        String messageKey = "error.validation.failed";
        String message = MessageUtil.getMessage(messageKey);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        String messageKey = "error.validation.failed";
        String message = MessageUtil.getMessage(messageKey);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String messageKey;
        if (ex.getMessage().contains("duplicate") || ex.getMessage().contains("unique")) {
            messageKey = "error.data.duplicate";
        } else {
            messageKey = "error.data.integrity";
        }
        String message = MessageUtil.getMessage(messageKey);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccess(InvalidDataAccessApiUsageException ex) {
        String messageKey = "error.data.invalid";
        String message = MessageUtil.getMessageOrDefault(messageKey, "Invalid data access");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        String messageKey = "error.auth.invalid_credentials";
        String message = MessageUtil.getMessage(messageKey);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleServiceException(ServiceException ex) {
        String messageKey = ex.getMessage();
        String message = MessageUtil.getMessageOrDefault(messageKey, messageKey);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex) {
        String messageKey = ex.getMessage();
        String message = MessageUtil.getMessageOrDefault(messageKey, messageKey);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicException(BusinessLogicException ex) {
        String messageKey = ex.getMessage();
        String message = MessageUtil.getMessageOrDefault(messageKey, messageKey);
        return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(messageKey, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        String messageKey = "error.server.internal";
        String message = MessageUtil.getMessage(messageKey);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(messageKey, message));
    }
}