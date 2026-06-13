package com.toni.FoodApp.exceptions;

public class OutOfStockException extends RuntimeException  {
    public OutOfStockException(String message) {
        super(message);
    }
}