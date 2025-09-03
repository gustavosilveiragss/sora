package com.sora.backend.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MessageUtil {

    private final MessageSource messageSource;

    public MessageUtil(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String getMessage(String key) {
        return messageSource.getMessage(key, null, key, LocaleContextHolder.getLocale());
    }

    public String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    public String getMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }

    public String getMessage(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, key, locale);
    }

    public String getValidationMessage(String field, String constraint) {
        String key = String.format("validation.%s.%s", field, constraint);
        return getMessage(key);
    }

    public String getEntityMessage(String entityName) {
        String key = String.format("entity.%s", entityName);
        return getMessage(key);
    }

    public String getErrorMessage(String errorType) {
        String key = String.format("error.%s", errorType);
        return getMessage(key);
    }

    public String getSuccessMessage(String operation) {
        String key = String.format("success.%s", operation);
        return getMessage(key);
    }
}