package com.toni.FoodApp.auth_users.services;

import com.toni.FoodApp.auth_users.Specification.UserSpecifications;
import com.toni.FoodApp.auth_users.dtos.ChangePasswordRequest;
import com.toni.FoodApp.auth_users.dtos.UserDTO;
import com.toni.FoodApp.auth_users.dtos.UserFilterRequest;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.repository.UserRepository;
import com.toni.FoodApp.aws.AWSS3Service;
import com.toni.FoodApp.email_notification.services.NotificationService;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.ForbiddenException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.exceptions.RestaurantMissingException;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import com.toni.FoodApp.role.entity.Role;
import com.toni.FoodApp.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final NotificationService notificationService;
    private final AWSS3Service awsS3Service;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    @Override
    public User getCurrentLoggedInUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public Response<List<UserDTO>> getAllUsers(UserFilterRequest filter) {
        log.info("Inside getAllUsers()");

        // The repository call is now clean and only passes the two required arguments
        List<User> users = userRepository.findAllByRoleNameAndSearch(
                filter.getRole(),
                filter.getSearchString()
        );


        List<UserDTO> userDTOS = modelMapper.map(users, new TypeToken<List<UserDTO>>(){}.getType());

        return Response.<List<UserDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Users retrieved successfully.")
                .data(userDTOS)
                .build();
    }

    @Override
    public Response<List<UserDTO>> getAllBranchManagers(Long id, String name, String branchName, Boolean isAssigned) {
        log.info("Inside getAllBranchManagers()");
        User currentUser = getCurrentLoggedInUser();

        if (currentUser.getRestaurant() == null) {
            throw new ForbiddenException("You are not assigned to a restaurant.");
        }

        Long restaurantId = currentUser.getRestaurant().getId();

        List<User> users = userRepository.findAll(
                UserSpecifications.filterBranchManagers(restaurantId, id, name, branchName, isAssigned)
        );

        List<UserDTO> userDTOS = modelMapper.map(users, new TypeToken<List<UserDTO>>(){}.getType());

        return Response.<List<UserDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Users retrieved successfully.")
                .data(userDTOS)
                .build();
    }


    @Override
    @Cacheable(value = "userProfileCache", key = "#user.id", sync = true)
    public Response<UserDTO> getOwnAccountDetails(User user) {
        log.info("Inside getOwnAccountDetails()");

        UserDTO data = modelMapper.map(user, UserDTO.class);
        data.setProfileUrl(user.getProfileUrl());
        if(user.getRestaurant() != null) data.setRestaurantId(user.getRestaurant().getId());
        return Response.<UserDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Profile data retrieved.")
                .data(data)
                .build();
    }

    @Override
    public Response<?> updateOwnAccount(UserDTO userDTO) {
        log.info("Inside updateOwnAccount");

        //fetch current logged in user
        User user = getCurrentLoggedInUser();
        MultipartFile imageFile = userDTO.getImageFile();
        if(imageFile != null && !imageFile.isEmpty()) {
            String newImageUrl = awsS3Service.replaceImage(imageFile, user.getProfileUrl(), "profile");
            user.setProfileUrl(newImageUrl);
        }
        //update user details. No image provided here
        if(userDTO.getName() != null) user.setName(userDTO.getName());
        if(userDTO.getPhoneNumber() != null) user.setPhoneNumber(userDTO.getPhoneNumber());
        if(userDTO.getAddress() != null) user.setAddress(userDTO.getAddress());
        if(userDTO.getNewPassword() != null && userDTO.getCurrentPassword() != null) {
            changePassword(user, userDTO.getNewPassword(), userDTO.getCurrentPassword());
        };

        //edit email if provided and if is different from the current one
        if (userDTO.getEmail() != null && !userDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(userDTO.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(userDTO.getEmail());
        }

        //save the user
        userRepository.save(user);
        return Response.<UserDTO> builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account updated successfully")
                .build();
    }

    public void changePassword(User user, String newPassword, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password incorrect");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
    }


    @Override
    public Response<?> changeRole(Long userId, RoleName newRoleName) {
        log.info("Inside changeRole()");

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return Response.builder()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("Not Found: User with id: " + userId + " not found. Please check the id and try again.")
                    .build();
        }

        User user = optionalUser.get();

        Optional<Role> optionalRole = roleRepository.findByName(newRoleName);
        if (optionalRole.isEmpty()) {
            return Response.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Bad Request: Role '" + newRoleName + "' does not exist.")
                    .build();
        }
        Role newRole = optionalRole.get();

        boolean hasSameRole = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(newRoleName));
        if (hasSameRole) {
            return Response.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Bad Request: User already has role '" + newRoleName + "'.")
                    .build();
        }

        // Remove all existing roles
        user.getRoles().clear();
        userRepository.save(user); // Important to update join table

        // Assign new role
        user.getRoles().add(newRole);
        userRepository.save(user);

        return Response.builder()
                .statusCode(200)
                .message("Role changed successfully to '" + newRoleName + "' for user id " + userId)
                .build();
    }



    @Override
    public Response<?> deactivateAccount(Long id) {
        User user = getUserAndValidateIfExistsAndIsUnactive(id,false);
        return removeAndReturn(user);

    }

    @Override
    @Transactional
    public Response<?> deactivateBranchManager(Long id) {
        User user = getUserAndValidateIfExistsAndIsUnactive(id,false);
        //make sure the user is role branch-manager and belongs to the company of the managers restaurant
        //get current manager id
        validateManagerRoleAndReturnRestaurant(user);
        //set the branch_manager_id in the restaurant_branch table to NULL
        branchRepository.findByManagerId(user.getId()).ifPresent(branch -> branch.setManager(null));
        return removeAndReturn(user);
    }

    @Override
    @Transactional
    public Response<?> restoreBranchManager(Long id) {
        User user = getUserAndValidateIfExistsAndIsUnactive(id,true);
        validateManagerRoleAndReturnRestaurant(user);
        return restoreUserAndSentEmail(user);
    }

    @Override
    @Transactional
    public Response<?> restoreUsers(Long id) {
        User user = getUserAndValidateIfExistsAndIsUnactive(id,true);
        return restoreUserAndSentEmail(user);
    }


    @Override
    @Transactional
    public Response<?> deactivateOwnAccountEmail() {
        User user = getCurrentLoggedInUser();
        return removeAndReturn(user);
    }

    @Override
    public void updateSelectedPaymentMethodAndSaveTheUser(User user, PaymentMethodEntity method) {
        user.setLastSelectedPaymentMethod(method);
        save(user);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    @Override
    public void requireRole(User user, RoleName roleName) {
        boolean hasRole = user.getRoles()
                .stream()
                .anyMatch(role -> roleName.equals(role.getName()));

        if (!hasRole) {
            throw new ForbiddenException(
                    "You must have role " + roleName + " to call this endpoint"
            );
        }
    }



    @Override
    public Restaurant getRestaurantIdByCurrentLoggedUser() {
        User user = this.getCurrentLoggedInUser();
        Restaurant restaurant = user.getRestaurant();

        // Check if the restaurant object itself is null
        if (restaurant == null) {
            throw new RestaurantMissingException("No restaurant associated with this user.");
        }

        return restaurant;
    }


    @Override
    public Response<?> changePassword(ChangePasswordRequest request) {
        User currentUser = getCurrentLoggedInUser();

        // SCENARIO 1: Voluntary change (User is already "fully" in)
        if (!currentUser.getRequirePasswordChange()) {
            if (request.getOldPassword() == null) {
                throw new BadRequestException("Old password is required for security verification.");
            }

            if (!passwordEncoder.matches(request.getOldPassword(), currentUser.getPassword())) {
                throw new BadRequestException("The old password provided is incorrect.");
            }
        }
        validateNewPassword(request.getNewPassword(), request.getConfirmNewPassword());

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        currentUser.setRequirePasswordChange(false); // Reset the flag for both scenarios
        userRepository.save(currentUser);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Password updated successfully.")
                .build();
    }

    //======================== Helper METHODS ========================//
    private void validateNewPassword(String newPass, String confirmPass) {
        if (newPass == null || newPass.isEmpty()) {
            throw new BadRequestException("New password cannot be empty.");
        }
        if (!newPass.equals(confirmPass)) {
            throw new BadRequestException("New password and confirmation do not match.");
        }
    }

    private Response<?> removeAndReturn(User user){
        user.setActive(false);
        userRepository.save(user);
        notificationService.deactivateAccount(user);
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account deactivated successfully.")
                .build();
    }
    private User getUserAndValidateIfExistsAndIsUnactive(Long id, Boolean restore){
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isEmpty()) {
            throw new NotFoundException("User not found with id: " + id + ".");
        }
        if(!restore){
            if(!optionalUser.get().isActive()) throw  new BadRequestException("The user is already unactive!");
        }
        else{
            if(optionalUser.get().isActive()) throw  new BadRequestException("The user is already active!");
        }
        return optionalUser.get();
    }
    private void validateManagerRoleAndReturnRestaurant(User user){
        if(!Objects.equals(user.getRoles().getFirst().getName(), RoleName.BRANCH_MANAGER)) throw new ForbiddenException("You cant deactivate non manger role.");
        Long restaurantId = getCurrentLoggedInUser().getRestaurant().getId();
        if(restaurantId == null) throw new ForbiddenException("Only managers can make this request!");
        if (!Objects.equals(user.getCreatedByCompany(), restaurantId)) throw new ForbiddenException("You can't deactivate/restore this user because he was not created by your managed company.");
    }


    private Response<?> restoreUserAndSentEmail(User user){
        user.setActive(true);
        userRepository.save(user);
        notificationService.deactivateAccount(user);
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Account restored successfully.")
                .build();
    }
    @Override
    public User getCurrentLoggedInUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return null;
        }
        return getCurrentLoggedInUser();
    }
}
