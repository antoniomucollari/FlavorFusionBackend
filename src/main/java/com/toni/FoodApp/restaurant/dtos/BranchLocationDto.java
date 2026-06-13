package com.toni.FoodApp.restaurant.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchLocationDto {//for manager panel
    private Long id;
    private String address;
    private Double latitude;
    private Double longitude;
}