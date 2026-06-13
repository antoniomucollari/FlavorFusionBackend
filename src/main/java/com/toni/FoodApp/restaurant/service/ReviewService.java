package com.toni.FoodApp.restaurant.service;

import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.dtos.ReviewDTO;
import com.toni.FoodApp.restaurant.dtos.response.ReviewSummaryDto;

import java.util.List;

public interface ReviewService {
    Response<?> createReview(ReviewDTO reviewDTO);
    Response<List<ReviewSummaryDto>> getReviewsForBranch(Long branchId);
    Response<Double> getAverageRating(Long menuId);
}
