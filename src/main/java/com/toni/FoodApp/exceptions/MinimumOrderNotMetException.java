package com.toni.FoodApp.exceptions;

public class MinimumOrderNotMetException extends BadRequestException {
    public MinimumOrderNotMetException(String message) {
        super(message);
    }
}