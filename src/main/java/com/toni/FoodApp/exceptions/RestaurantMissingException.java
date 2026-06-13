package com.toni.FoodApp.exceptions;

public class RestaurantMissingException extends ForbiddenException {
    public RestaurantMissingException(String s) {
        super(s);
    }
}
