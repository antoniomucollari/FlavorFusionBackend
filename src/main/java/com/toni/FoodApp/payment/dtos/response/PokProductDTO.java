package com.toni.FoodApp.payment.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PokProductDTO {
    private String name;
    private int quantity;
    private long price;
}