package com.toni.FoodApp.exceptions;

import com.toni.FoodApp.response.Response;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException; // <-- NEW
import org.springframework.validation.FieldError; // <-- NEW
import org.springframework.web.bind.MethodArgumentNotValidException; // <-- NEW
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap; // <-- NEW
import java.util.Map; // <-- NEW
import java.util.stream.Collectors;
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- YOUR EXISTING HANDLERS (All Good!) ---

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Response<?>> handleNotFoundExceptions(NotFoundException ex){
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Response<?>> badRequestExceptions(BadRequestException ex){
        // This will now ALSO catch your custom checkout exceptions
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<Response<?>> paymentProcessingExceptions(PaymentProcessingException ex){
        return createErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), null);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Response<?>> unauthorizedAccessExceptions(UnauthorizedAccessException ex){
        return createErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Response<?>> illegalArgumentExceptions(IllegalArgumentException ex){
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    // --- NEW RECOMMENDED HANDLERS ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {

        if (ex.getMessage().contains("manager_id")) {
            return createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "This manager is already assigned to another branch.",
                    null
            );
        }

        return createErrorResponse(
                HttpStatus.CONFLICT,
                "A data integrity violation occurred.",
                null
        );
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Response<?>> handleAccessDenied(AccessDeniedException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", null);
    }

    // --- YOUR CATCH-ALL HANDLERS (Good) ---

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Response<?>> illegalStateExceptions(IllegalStateException ex, HttpServletResponse response){
        logger.error("Illegal state exception occurred: ", ex);
        if (response.isCommitted()) {
            return null;
        }
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<?>> handleAllUnknownExceptions(Exception ex, WebRequest request, HttpServletResponse response){
        logger.error("Unexpected error occurred: ", ex);
        if (response.isCommitted()) {
            return null;
        }
        return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }


    private ResponseEntity<Response<?>> createErrorResponse(HttpStatus status, String message, Object data) {
        Response<?> response = Response.builder()
                .statusCode(status.value())
                .message(message)
                .data(data) // Assumes your Response object has a 'data' field
                .build();
        return new ResponseEntity<>(response, status);
    }
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Response<?>> handleForbidden(ForbiddenException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }
    @ExceptionHandler(RestaurantMissingException.class)
    public ResponseEntity<Object> handleRestaurantMissing(RestaurantMissingException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", HttpStatus.FORBIDDEN.value());
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<Object> handleOutOfStock(OutOfStockException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Stock Error");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Response<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String invalidValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        Class<?> requiredType = ex.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "unknown";

        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                invalidValue, paramName, typeName
        );

        return createErrorResponse(HttpStatus.BAD_REQUEST, message, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> handleMissingParam(MissingServletRequestParameterException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("error", "Missing request parameter");
        error.put("parameter", ex.getParameterName());

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<Map<String, String>> handlePaymentGatewayException(PaymentGatewayException ex) {
        log.warn("Payment Gateway Error: {}", ex.getMessage());

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Payment Error");
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


}