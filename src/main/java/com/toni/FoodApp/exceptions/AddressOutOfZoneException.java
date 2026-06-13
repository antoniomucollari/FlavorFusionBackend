package com.toni.FoodApp.exceptions;

public class AddressOutOfZoneException extends BadRequestException {
    public AddressOutOfZoneException(String message) {
        super(message);
    }
}