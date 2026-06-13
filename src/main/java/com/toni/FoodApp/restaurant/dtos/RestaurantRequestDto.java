package com.toni.FoodApp.restaurant.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
public class RestaurantRequestDto { //for post request
    private String name;
    private String description;
    private MultipartFile coverImage;
    private MultipartFile profileImage;
    private String phoneNumber;
    private boolean isPromoted = false;
    private List<Long> categories;//ids
}
