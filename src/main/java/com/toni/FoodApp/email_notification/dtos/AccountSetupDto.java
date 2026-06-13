package com.toni.FoodApp.email_notification.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountSetupDto {
    private String email;
    private String password;
}
