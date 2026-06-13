package com.toni.FoodApp.exceptions;

public class CartIsEmptyException extends BadRequestException {
    public CartIsEmptyException(String message) {
        super(message);
    }
}
