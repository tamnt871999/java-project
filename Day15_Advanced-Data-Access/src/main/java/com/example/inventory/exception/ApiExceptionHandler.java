package com.example.inventory.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError badRequest(Exception exception) {
        return new ApiError("BAD_REQUEST", "Du lieu gui len khong hop le");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError typeMismatch(MethodArgumentTypeMismatchException exception) {
        return new ApiError("BAD_REQUEST", "Tham so '" + exception.getName() + "' phai la so nguyen");
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(InventoryNotFoundException exception) {
        return new ApiError("INVENTORY_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InventoryVersionConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError versionConflict(InventoryVersionConflictException exception) {
        return new ApiError("VERSION_CONFLICT", exception.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError concurrentUpdate(OptimisticLockingFailureException exception) {
        return new ApiError("VERSION_CONFLICT", "San pham vua bi nguoi khac sua, hay doc lai roi thu lai");
    }

    public record ApiError(String code, String message) {
    }
}
