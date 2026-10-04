package com.common.storage.exception;

import com.common.storage.model.dto.ResponseModel;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@ControllerAdvice
public class StorageGlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ResponseModel<Void>> handleException(Exception e) {
        log.error("Exception: " + e.getMessage() + " with type: " + e.getClass());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = ServletException.class)
    public ResponseEntity<ResponseModel<Void>> handleServletException(ServletException e) {
        log.error("Exception: " + e.getMessage() + " with type: " + e.getClass());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = MaxUploadSizeExceedException.class)
    public ResponseEntity<ResponseModel<Void>> handleMaxUploadSizeExceedException(
            MaxUploadSizeExceedException e) {
        log.warn("Exception: " + e.getMessage() + " with type: " + e.getClass());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = MissingServletRequestPartException.class)
    public ResponseEntity<ResponseModel<Void>> handleMissingServletRequestPartException(Exception e) {
        log.warn("Exception: " + e.getMessage() + " with type: " + e.getClass());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = MultipartException.class)
    public ResponseEntity<ResponseModel<Void>> handleMultipartException(Exception e) {
        log.warn("Exception: " + e.getMessage() + " with type: " + e.getClass());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseModel<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        FieldError error = ex.getBindingResult().getFieldError();
        String fieldName = error.getField();
        String errorMessage = error.getDefaultMessage();
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, fieldName + ": " + errorMessage), HttpStatus.OK);
    }

    @ExceptionHandler(value = IllegalArgumentException.class)
    public ResponseEntity<ResponseModel<Void>> handleIllegalArgumentException(Exception e) {
        log.warn("Exception: " + e.getMessage() + " with type: " + e.getClass());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = ForbiddenException.class)
    public ResponseEntity<ResponseModel<Void>> handleForbiddenException(Exception e) {
        log.error("Exception: " + e.getMessage() + " with type: " + e.getClass());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = InvalidFileTypeException.class)
    public ResponseEntity<ResponseModel<Void>> handleInvalidFileTypeException(Exception e) {
        log.warn("Exception: " + e.getMessage() + " with type: " + e.getClass());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseModel<Void>> handleMaxUploadSizeExceededException(Exception e) {
        log.warn("Exception: " + e.getMessage() + " with type: " + e.getClass());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = EntityNotFoundException.class)
    public ResponseEntity<ResponseModel<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseModel<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, "Invalid request body"), HttpStatus.OK);
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseModel<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, "Invalid Id"), HttpStatus.OK);
    }

    @ExceptionHandler(value = NotFoundException.class)
    public ResponseEntity<ResponseModel<Void>> handleNotFoundExceptionException(NotFoundException e) {
        log.warn("NotFoundException: {}", e.getMessage());
        return new ResponseEntity<>(
                new ResponseModel<>(false, null, e.getMessage()), HttpStatus.OK);
    }

}
