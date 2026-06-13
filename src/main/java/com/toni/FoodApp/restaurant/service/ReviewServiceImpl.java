package com.toni.FoodApp.restaurant.service;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserServiceImpl;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.restaurant.dtos.response.ReviewSummaryDto;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import com.toni.FoodApp.restaurant.repository.RestaurantRepository;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.order.repository.OrderItemRepository;
import com.toni.FoodApp.order.repository.OrderRepository;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.dtos.ReviewDTO;
import com.toni.FoodApp.restaurant.entity.Review;
import com.toni.FoodApp.restaurant.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService{
    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private final BranchRepository branchRepository;
    private final OrderItemRepository orderItemRepository;
    private final ModelMapper modelMapper;
    private final UserServiceImpl userService;
    @Override

    @Transactional
    public Response<?> createReview(ReviewDTO reviewDTO) {
        log.info("Inside createReview()");

        User user = userService.getCurrentLoggedInUser();
        Order order = orderRepository.findById(reviewDTO.getOrderId()).orElseThrow(() -> new NotFoundException("Order not found"));

        if(!order.getUser().getId().equals(user.getId())){
            throw new BadRequestException("Order does not belong to the current logged in user");

        }
        if(order.getOrderStatus() != OrderStatus.DELIVERED) throw new BadRequestException("Order must be in DELIVERED status to be reviewed");
        //check if the user already wrote a review for this item
        if(reviewRepository.existsByUserIdAndOrderId(
                user.getId(), reviewDTO.getOrderId()
        ))throw new BadRequestException("User already wrote a review for this item");

        Review review = Review.builder()
                .user(user)
                .rating(reviewDTO.getRating())
                .comment(reviewDTO.getComment())
                .createdAt(LocalDateTime.now())
                .branch(order.getBranch())
                .order(order)
                .build();
        reviewRepository.save(review);

        return Response.builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Review successfully created!")
                .build();
    }

    @Override
    public Response<List<ReviewSummaryDto>> getReviewsForBranch(Long restaurantId) {
        java.util.List<Review> reviewList = reviewRepository.findByBranchIdOrderByIdDesc(restaurantId);

        List<ReviewSummaryDto> reviewDtos = reviewList.stream()
                .map(this::mapReviewToDto) // Call a helper method
                .toList();

        return Response.<List<ReviewSummaryDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Reviews successfully retrieved!") // (I also fixed your success message)
                .data(reviewDtos)
                .build();
    }

    //mapper
    private ReviewSummaryDto mapReviewToDto(Review review) {
        // You can still use ModelMapper for the simple fields!
        String userName = review.getUser().getName();

        if (userName != null && userName.length() > 3) {
            int starsCount = userName.length() - 3;
            String stars = "*".repeat(starsCount);
            userName = userName.substring(0, 3) + stars;
        }
        return ReviewSummaryDto.builder()
                .id(review.getId())
                .userName(userName)
                .createdAt(review.getCreatedAt())
                .rating(review.getRating())
                .comment(review.getComment())
                .build();
    }

    @Override
    public Response<Double> getAverageRating(Long restaurantId) {
        Double averageRating = reviewRepository.calculateAverageRatingByRestaurantId(restaurantId);
        return Response.<Double>builder()
                .statusCode(HttpStatus.OK.value())
                .message("restaurant successfully created!")
                .data(averageRating != null ? averageRating : 0.0)
                .build();
    }

    //TODO add a dedicated scheduler
//    @Scheduled(fixedRate = 15 * 60 * 1000)
//    @Transactional
//    public void updateAllBranchRatings() {
//        log.info("Starting background job to update all branch ratings...");
//
//        List<RestaurantBranch> branches = branchRepository.findAll();
//        for (RestaurantBranch branch : branches) {
//            double newRating = reviewRepository.getAverageRatingByBranchId(branch.getId());
//            Long newCount = reviewRepository.countByBranchId(branch.getId());
//
//            branch.setAverageRating(BigDecimal.valueOf(newRating));
//            branch.setReviewCount(newCount.intValue());
//
//            branchRepository.save(branch); // Save the updated values
//        }
//        log.info("Finished updating all branch ratings.");
//    }
}
