package com.toni.FoodApp.deliverTo.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LocationPayload {
    private double latitude;
    private double longitude;
}
