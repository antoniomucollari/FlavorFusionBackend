package com.toni.FoodApp.auth_users.controller;

import com.toni.FoodApp.auth_users.dtos.LoginRequest;
import com.toni.FoodApp.auth_users.dtos.LoginResponse;
import com.toni.FoodApp.auth_users.dtos.RegistrationRequest;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.repository.UserRepository;
import com.toni.FoodApp.auth_users.services.AuthService;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.ForbiddenException;
import com.toni.FoodApp.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor //only works when there are final fields
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register-customer")
    public ResponseEntity<Response<?>> registerCustomer(@RequestBody @Valid RegistrationRequest registrationRequest){
        return ResponseEntity.ok(authService.registerCustomer(registrationRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<Response<LoginResponse>> login(@RequestBody @Valid LoginRequest loginRequest){
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));
        return ResponseEntity.ok(authService.login(loginRequest,user));
    }

    @PostMapping("/login-delivery")
    public ResponseEntity<Response<LoginResponse>> loginDelivery(@RequestBody @Valid LoginRequest loginRequest){
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));
        if(!user.getRoles().getFirst().getName().equals(RoleName.DELIVERY)) throw new  ForbiddenException("This endpoint is for delivery only.");
        return ResponseEntity.ok(authService.login(loginRequest, user));
    }

    @PostMapping("/register-manager")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<?>> createManager(@RequestBody @Valid RegistrationRequest registrationRequest){
        return ResponseEntity.ok(authService.createManager(registrationRequest));
    }

    @PostMapping("/register-branch-manager")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> createBranchManager(@RequestBody @Valid RegistrationRequest registrationRequest){
        return ResponseEntity.ok(authService.createBranchManager(registrationRequest));
    }

}
