package com.toni.FoodApp.auth_users.controller;

import com.toni.FoodApp.auth_users.dtos.ChangePasswordRequest;
import com.toni.FoodApp.auth_users.dtos.UserDTO;
import com.toni.FoodApp.auth_users.dtos.UserFilterRequest;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor //only works when there are final fields
public class UserController {
    private final UserService userService;

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('DELIVERY') or hasAuthority('MANAGER')")
    public ResponseEntity<Response<List<UserDTO>>> getAllUsers(
            @ModelAttribute UserFilterRequest filter) {

        return ResponseEntity.ok(
                userService.getAllUsers(filter)
        );
    }

    @GetMapping("/branch_managers")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<List<UserDTO>>> getAllBranchManagers(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String branchName,
            @RequestParam(required = false) Boolean isAssigned
    ) {

        return ResponseEntity.ok(
                userService.getAllBranchManagers(id, name, branchName, isAssigned)
        );
    }

    @GetMapping("/change-role")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<?>> changeRole(
            @RequestParam() Long id,
            @RequestParam(defaultValue = "DELIVERY") RoleName changeTo) {
        return ResponseEntity.ok(userService.changeRole(id,changeTo));
    }


    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Response<?>> updateOwnAccount(
            @ModelAttribute UserDTO userDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile){
        userDTO.setImageFile(imageFile);
        return ResponseEntity.ok(userService.updateOwnAccount(userDTO));
    }

    @DeleteMapping("/deactivate-any")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<?>> deactivateAnyAccount(@RequestParam(required = true) Long id){
        return ResponseEntity.ok(userService.deactivateAccount(id));
    }
    @DeleteMapping("/deactivate-branch-managers")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> deactivateBranchManagers(@RequestParam(required = true) Long id){
        return ResponseEntity.ok(userService.deactivateBranchManager(id));
    }

    @PostMapping("/restore-branch-managers")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> restoreBranchManagers(@RequestParam(required = true) Long id){
        return ResponseEntity.ok(userService.restoreBranchManager(id));
    }

    @PostMapping("/restore-users")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Response<?>> restoreUsers(@RequestParam(required = true) Long id){
        return ResponseEntity.ok(userService.restoreUsers(id));
    }

    @DeleteMapping("/deactivate-my-account")
    public ResponseEntity<Response<?>> deactivateMyOwnAccount(){
        return ResponseEntity.ok(userService.deactivateOwnAccountEmail());
    }

    @GetMapping("/account")
    public ResponseEntity<Response<UserDTO>> getOwnAccountDetails(){
        User user = userService.getCurrentLoggedInUser();
        return ResponseEntity.ok(userService.getOwnAccountDetails(user));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<Response<?>> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest){
        return ResponseEntity.ok(userService.changePassword(changePasswordRequest));
    }
}
