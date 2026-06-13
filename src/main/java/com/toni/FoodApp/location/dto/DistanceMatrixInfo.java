package com.toni.FoodApp.location.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DistanceMatrixInfo implements Serializable {
    private long distanceInMeters;
    private long durationInSeconds;
}