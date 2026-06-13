package com.toni.FoodApp.auth_users.dtos;

import com.toni.FoodApp.enums.RoleName;
import lombok.Data;

import java.util.List;

@Data
public class LoginResponse {
    private String token;
    private List<RoleName> roles;
    private String name;
}
