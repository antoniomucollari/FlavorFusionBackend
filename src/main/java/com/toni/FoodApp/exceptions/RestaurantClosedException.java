package com.toni.FoodApp.exceptions;

public class RestaurantClosedException extends BadRequestException {
    public RestaurantClosedException(String message) {
        super(message);
    }
}
