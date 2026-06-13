package com.toni.FoodApp.exceptions;

public class UnsupportedPaymentMethodException extends BadRequestException {
    public UnsupportedPaymentMethodException(String message) {
        super(message);
    }
}
