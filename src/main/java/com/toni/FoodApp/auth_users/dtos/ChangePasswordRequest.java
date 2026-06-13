package com.toni.FoodApp.auth_users.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ChangePasswordRequest {

    private String oldPassword;      // required only if firstLoginAttempt == false
    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$",
            message = "NewPassword must contain uppercase, lowercase, and a number"
    )
    private String newPassword;    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$",
            message = "Confirm password must contain uppercase, lowercase, and a number"
    )
    private String confirmNewPassword;
}
