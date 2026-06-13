package com.toni.FoodApp.auth_users.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.role.entity.Role;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    private Long id;
    private String name;
    private String email;


    private String phoneNumber;
    private String profileUrl;
    private String address;
    private boolean isActive;
    private List<Role> roles;
    @JsonIgnore
    private List<Order> orders;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private MultipartFile imageFile;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)// if Java → JSON this will not appear
    private String currentPassword; //when the user wants to change to new password

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String newPassword;

    private Long restaurantId;
    private Long restaurantBranchId;

}
