package com.toni.FoodApp.restaurant.dtos.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewSummaryDto {
    private Long id;
    private String userName;
    @Min(1)
    @Max(5)
    private Integer rating;
    @Size(max = 500, message = "Comment cannot exceed 500 characters.")
    private String comment;
    private LocalDateTime createdAt;
}

