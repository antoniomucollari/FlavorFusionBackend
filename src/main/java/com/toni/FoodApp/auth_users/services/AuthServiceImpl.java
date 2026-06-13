package com.toni.FoodApp.auth_users.services;

import com.toni.FoodApp.auth_users.dtos.LoginRequest;
import com.toni.FoodApp.auth_users.dtos.LoginResponse;
import com.toni.FoodApp.auth_users.dtos.RegistrationRequest;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.mapper.UserMapper;
import com.toni.FoodApp.auth_users.repository.UserRepository;
import com.toni.FoodApp.email_notification.dtos.AccountSetupDto;
import com.toni.FoodApp.email_notification.services.NotificationService;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.exceptions.RestaurantMissingException;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.payment.repository.PaymentMethodRepository;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import com.toni.FoodApp.restaurant.service.BranchService;
import com.toni.FoodApp.role.entity.Role;
import com.toni.FoodApp.role.repository.RoleRepository;
import com.toni.FoodApp.security.JwtUtils;
import com.toni.FoodApp.security.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RoleRepository roleRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PasswordUtils passwordUtils;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final BranchService branchService;
    @Value("${app.default.payment-method-id}")
    private Long defaultPaymentMethodId;
    @Override
    public Response<?> registerCustomer(RegistrationRequest registrationRequest) {
        log.info("Inside customer register()");
        checkIfUserExists(registrationRequest.getEmail());
        List<Role> userRoles = getUserRoles(RoleName.CUSTOMER);
        PaymentMethodEntity defaultMethod = getDefaultPaymentMethod();

        User userToSave = userMapper.mapToUser(registrationRequest, userRoles, passwordEncoder.encode(registrationRequest.getPassword()), defaultMethod, false);
        //save the user
        userRepository.save(userToSave);
        log.info("User saved successfully.");
        return Response.builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("User registered successfully.").build();

    }

    @Override
    public Response<LoginResponse> login(LoginRequest loginRequest, User user) {
        log.info("INSIDE login()");
//        Role role = user.getRoles().stream().findFirst().orElseThrow();
//        if(!role.getName().equalsIgnoreCase("CUSTOMER")){
//            throw new BadRequestException("Only customers can login here");
//        }

        //check if user is not active
        if(!user.isActive()){
            throw new NotFoundException("Account is not active");
        }

        //verify the password
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new BadRequestException("Invalid credentials");
        }

        //generate token
        String token = jwtUtils.generateToken(user.getEmail(), user.getTokenVersion());
        //Extract roles
        List<RoleName> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setToken(token);
        loginResponse.setRoles(roles);
        loginResponse.setName(user.getName());
        log.info("User logged in successfully.");

        return Response.<LoginResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("User logged in successfully.")
                .data(loginResponse)
                .build();

    }

    @Override
    public Response<?> createManager(RegistrationRequest registrationRequest){
        log.info("Inside manager register()");
        checkIfUserExists(registrationRequest.getEmail());
        List<Role> userRoles = getUserRoles(RoleName.MANAGER);
        String password = passwordUtils.generateRandomPassword(8);

        //map to user
        User mapToUser = userMapper.mapToUser(registrationRequest, userRoles, passwordEncoder.encode(password), null, true);

        //send an email with the password
        AccountSetupDto accountSetupDto = AccountSetupDto.builder().email(registrationRequest.getEmail()).password(password).build();
        notificationService.sendAccountSetupEmail(accountSetupDto, RoleName.MANAGER);

        //save the user
        userRepository.save(mapToUser);
        return Response.builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Manager registered successfully. The email with the temp password is sent in their email.")
                .build();

    }
    @Transactional
    @Override
    public Response<?> createBranchManager(RegistrationRequest registrationRequest) {
        log.info("Inside branch_manager register()");
        if (registrationRequest.getBranchId() == null) {
            throw new BadRequestException("Branch id is required");
        }
        checkIfUserExists(registrationRequest.getEmail());
        List<Role> userRoles = getUserRoles(RoleName.BRANCH_MANAGER);

        String password = passwordUtils.generateRandomPassword(8);
        if(registrationRequest.getBranchId() == null)throw new BadRequestException("Branch id is required");
        //map to user
        User mapToUser = userMapper.mapToUser(registrationRequest, userRoles, passwordEncoder.encode(password), null, true);
        Long restaurantId = userRepository.findRestaurantIdByBranchId(registrationRequest.getBranchId()).orElseThrow(() -> new RestaurantMissingException("Could not find the company to link this user to."));
        //save the user
        mapToUser.setCreatedByCompany(restaurantId);
        userRepository.save(mapToUser);

        //assign the branch manager role to the user
        branchService.assignBranchManagerAndValidate(registrationRequest.getBranchId(), mapToUser);
        AccountSetupDto accountSetupDto = AccountSetupDto.builder().email(registrationRequest.getEmail()).password(password).build();
        //send an email with the password
        notificationService.sendAccountSetupEmail(accountSetupDto, RoleName.BRANCH_MANAGER);
        return Response.builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Branch_Manager registered successfully. The email with the temp password is sent in their email.")
                .build();
    }


    private void checkIfUserExists(String email){
        if(userRepository.existsByEmail(email)){
            throw new BadRequestException("User with email: " + email + " already exists");
        }
    }

    private List<Role> getUserRoles(RoleName roleName){
        return List.of(roleRepository.findByName(roleName).orElseThrow(()-> new NotFoundException(roleName + " role is not found. Please add it to the database first.")));
    }

    private PaymentMethodEntity getDefaultPaymentMethod(){
        return paymentMethodRepository.findById(defaultPaymentMethodId)
                .orElseThrow(() -> new RuntimeException("Default payment method not found in database!"));

    }

}
