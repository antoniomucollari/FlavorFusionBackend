package com.toni.FoodApp.restaurant.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpeningHourDto {

    @NotNull(message = "Day of week cannot be null")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Open time cannot be null")
    private LocalTime openTime;

    @NotNull(message = "Close time cannot be null")
    private LocalTime closeTime;
}