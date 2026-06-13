package com.toni.FoodApp.restaurant.service;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.dtos.*;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;

import java.math.BigDecimal;
import java.util.List;

public interface BranchService {

    Response<?> saveOpeningHours(OpeningHoursRequestDto dto);
    Response<?> editBranch(Long branchId, BranchRequestDto branchDto);

    Response<?> createBranch(BranchRequestDto branchDto);

    Response<RestaurantBranchDetailsDto> getById(Long id);

    Response<List<CategoryDTO>> getMenusByBranchId(Long branchId, String searchString);

    List<PaymentMethodEntity> getAvailablePaymentMethods(Long branchId);

    Boolean subtotalIsHigherThanMinOrderAmount(Long branchId, BigDecimal subtotal);

    RestaurantBranch getBranchEntityById(Long branchId);

    List<RestaurantBranch> getBranchesByRestaurantId(Long restaurantId);

    Response<?> changeManager(Long branchId, Long userId);

    Response<?> deleteBranch(Long branchId);

    Response<?> restoreBranch(Long branchId);
    void assignBranchManagerAndValidate(Long branchId, User manager);
    Response<RestaurantBranchDetailsDto> getMyBranch();

    Response<Boolean> changeOpeningStatus();

    Response<?> editMyBranch(BranchOperationsRequest dto);
    RestaurantBranch getBranchFromCurrentUser();


    Response<BranchLocationDto> getBranchLocation(Long branchId);
}
