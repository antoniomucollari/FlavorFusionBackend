package com.toni.FoodApp.cart.services;

import com.toni.FoodApp.cart.dtos.AddItemToCartRequest;
import com.toni.FoodApp.cart.dtos.CartDTO;
import com.toni.FoodApp.cart.dtos.ResponseCartDto;
import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.response.Response;

import java.math.BigDecimal;
import java.util.Optional;

public interface CartService {
    Response<?> addItemToCart(AddItemToCartRequest cartDTO);
    Response<ResponseCartDto> incrementItem(Long cartItemId);
    Response<ResponseCartDto> decrementItem(Long cartItemId);
    Response<ResponseCartDto> removeItem(Long cartItemId);
    Response<ResponseCartDto> getShoppingCart(Long branchId);
    Response<?> clearShoppingCart(Long branchId, Long userId);

    //helper method
    Cart getOrCreateCartForCurrentUserAndBranch(Long branchId);
    BigDecimal calculateSubtotal(Cart cart);
    Cart getCartForCheckout(Long branchId, Long userId);
    Optional<Cart> findCartByUserIdAndBranchId(Long userId, Long branchId);

    void save(Cart cart);
    void updateSelectedPaymentMethodAndSaveTheCart(Cart cart, PaymentMethodEntity method);

    Cart getCartAndValidate(Long cartId);

    Response<Long> orderAgain(Long orderId);
}
