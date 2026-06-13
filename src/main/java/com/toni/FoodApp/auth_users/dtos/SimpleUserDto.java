package com.toni.FoodApp.auth_users.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SimpleUserDto {
    private Long id;
    private String name;
}
