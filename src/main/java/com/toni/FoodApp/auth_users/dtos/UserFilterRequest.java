package com.toni.FoodApp.auth_users.dtos;

import com.toni.FoodApp.enums.RoleName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class UserFilterRequest {
    private RoleName role = RoleName.CUSTOMER;
    private String searchString = "";
}