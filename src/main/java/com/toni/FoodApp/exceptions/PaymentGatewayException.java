package com.toni.FoodApp.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for errors that occur while communicating with a payment gateway.
 * The @ResponseStatus annotation will cause Spring Boot to respond with a 502 Bad Gateway
 * status code whenever this exception is thrown and not caught elsewhere.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PaymentGatewayException extends RuntimeException {
    public PaymentGatewayException(String message) {
        super(message);
    }

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}