package com.example.inventory.adapter.in.web;

import com.example.inventory.application.port.in.SkuNotFoundException;
import com.example.inventory.application.port.out.ClusterUnavailableException;
import com.example.inventory.domain.DomainException;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse handleBadRequest(Exception exception) {
        return new ErrorResponse("BAD_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(SkuNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse handleSkuNotFound(SkuNotFoundException exception) {
        return new ErrorResponse("SKU_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ErrorResponse handleDomain(DomainException exception) {
        return new ErrorResponse("BUSINESS_RULE_VIOLATED", exception.getMessage());
    }

    @ExceptionHandler(ClusterUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    ErrorResponse handleClusterUnavailable(ClusterUnavailableException exception) {
        return new ErrorResponse("CLUSTER_UNAVAILABLE", exception.getMessage());
    }

    record ErrorResponse(String code, String message) {
    }
}
