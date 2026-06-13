package com.toni.FoodApp.restaurant.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SimpleBranchDto {//for manager panel
    private Long id;
    private String address;
    private Integer minOrderAmount;
    private Integer avgPrepTimeInMinutes;
    private boolean isClosed;
    private Integer averageRating;
    private Boolean deleted;
    private String managerName;
    private Long managerId;
}
