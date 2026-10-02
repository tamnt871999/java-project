package com.example.wallet.adapter.in.web;

import com.example.wallet.application.port.in.WalletNotFoundException;
import com.example.wallet.domain.DomainException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    ResponseEntity<ErrorResponse> handleNotFound(WalletNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ErrorResponse> handleDomainRule(DomainException exception) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATED", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException exception) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Body khong phai JSON hop le");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleBadPathVariable(MethodArgumentTypeMismatchException exception) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Tham so khong hop le: " + exception.getName());
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message));
    }

    record ErrorResponse(String code, String message) {
    }
}
