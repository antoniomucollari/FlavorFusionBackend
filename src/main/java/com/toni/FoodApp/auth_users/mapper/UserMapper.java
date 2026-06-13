package com.toni.FoodApp.auth_users.mapper;

import com.toni.FoodApp.auth_users.dtos.RegistrationRequest;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.role.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
@Component
@Slf4j
@RequiredArgsConstructor
public class UserMapper {

    public User mapToUser (RegistrationRequest registrationRequest, List<Role> userRoles, String password, PaymentMethodEntity defaultPaymentMethod, Boolean requiresPassChange){
        return User.builder()
                .name(registrationRequest.getName())
                .email(registrationRequest.getEmail())
                .phoneNumber(registrationRequest.getPhoneNumber())
                .address(registrationRequest.getAddress())
                .password(password)
                .roles(userRoles)
                .lastSelectedPaymentMethod(defaultPaymentMethod)
                .isActive(true)
                .requirePasswordChange(requiresPassChange)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
