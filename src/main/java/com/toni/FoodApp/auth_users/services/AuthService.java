package com.toni.FoodApp.auth_users.services;

import com.toni.FoodApp.auth_users.dtos.LoginRequest;
import com.toni.FoodApp.auth_users.dtos.LoginResponse;
import com.toni.FoodApp.auth_users.dtos.RegistrationRequest;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.response.Response;
import jakarta.validation.Valid;

public interface AuthService {
    Response<?> registerCustomer(RegistrationRequest registrationRequest);
    Response<LoginResponse> login(LoginRequest registrationRequest, User user);
    Response<?> createManager(RegistrationRequest registrationRequest);

    Response<?> createBranchManager(@Valid RegistrationRequest registrationRequest);


}
