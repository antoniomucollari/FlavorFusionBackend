package com.toni.FoodApp.deliverTo.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryLocationDTO {
    private Long id;
    private double latitude;
    private double longitude;
    private String locationName;
    private String nickname;
    private Long userId;
    private Boolean isDefault;
}