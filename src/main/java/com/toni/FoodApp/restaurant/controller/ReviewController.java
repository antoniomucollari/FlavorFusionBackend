package com.toni.FoodApp.restaurant.controller;

import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.dtos.ReviewDTO;
import com.toni.FoodApp.restaurant.dtos.response.ReviewSummaryDto;
import com.toni.FoodApp.restaurant.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/reviews", "/api/reviews/"})
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping("/{branchId}")
    public ResponseEntity<Response<List<ReviewSummaryDto>>> getReviewForRestaurant(@PathVariable Long branchId){
        return ResponseEntity.ok(reviewService.getReviewsForBranch(branchId));
    }
//    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<Response<?>> createReview(@RequestBody @Valid ReviewDTO reviewDTO){
        return ResponseEntity.ok(reviewService.createReview(reviewDTO));
    }

    @GetMapping("menu-item/{menuId}")
    public ResponseEntity<Response<Double>> getAverageReview(@PathVariable Long menuId){
        return ResponseEntity.ok(reviewService.getAverageRating(menuId));
    }
}
