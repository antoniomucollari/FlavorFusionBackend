package com.toni.FoodApp.cart.services;

import com.toni.FoodApp.cart.dtos.CartItemDTO;
import com.toni.FoodApp.cart.dtos.CheckoutCartDto;
import com.toni.FoodApp.cart.dtos.ResponseCartDto;
import com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO;
import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.cart.entity.CartItem;
import com.toni.FoodApp.cart.mapper.CartMapper;
import com.toni.FoodApp.deliverTo.dtos.DeliveryInfo;
import com.toni.FoodApp.menu.dtos.MenuOptionFlatDTO;
import com.toni.FoodApp.menu.dtos.VariantDTO;
import com.toni.FoodApp.menu.entity.Menu;
import com.toni.FoodApp.menu.repository.OptionRepository;
import com.toni.FoodApp.menu.repository.OptionVariantRepository;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartViewBuilderService {
    private final CartMapper cartMapper;
    private final OptionVariantRepository optionVariantRepository;
    private final OptionRepository optionRepository;


    public ResponseCartDto buildCartView(Cart cart) {
        RestaurantBranch branch = cart.getRestaurantBranch();
        Map<Long, VariantAvailabilityDTO> finalAvailabilityMap = getAvailableVariants(cart);
        List<CartItemDTO> itemDTOs = cart.getCartItems().stream()
                .map(item -> {
                    List<MenuOptionFlatDTO> menuOptionFlatDTO = optionRepository.findAvailableOptionsRaw(branch.getId(), item.getBranchMenuItem().getMenu().getId());

                    return cartMapper.toDto(item, finalAvailabilityMap);
                })
                .collect(Collectors.toList());

        // 2. Calculate Subtotal
        BigDecimal subtotal = itemDTOs.stream()
                .map(CartItemDTO::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Build the simple DTO
        return ResponseCartDto.builder()
                .id(cart.getId())
                .restaurantBranchId(branch.getId())
                .restaurantBranchName(branch.getRestaurant().getName())
                .items(itemDTOs)
                .subtotal(subtotal)
                .build();
    }
    public Map<Long, VariantAvailabilityDTO> getAvailableVariants(Cart cart) {
        Set<Long> allVariantIds = cart.getCartItems().stream()
                .flatMap(item -> item.getCartItemVariants().stream())
                .map(civ -> civ.getOptionVariant().getId())
                .collect(Collectors.toSet());

        if (allVariantIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<VariantAvailabilityDTO> statusList = optionVariantRepository.getAvailabilityStatus(
                allVariantIds,
                cart.getRestaurantBranch().getId()
        );

        // CHANGE HERE: Map the ID to the *entire DTO* (which includes price)
        return statusList.stream()
                .collect(Collectors.toMap(
                        VariantAvailabilityDTO::getId,
                        dto -> dto // Value is the DTO itself
                ));
    }
//    public CheckoutCartDto buildCartViewWithDelivery(Cart cart, double userLat, double userLon) {
//
//        RestaurantBranch branch = cart.getRestaurantBranch();
//
//        List<CartItemDTO> itemDTOs = cart.getCartItems().stream()
//                .map(cartMapper::toDto)
//                .collect(Collectors.toList());
//
//
//        BigDecimal subtotal = itemDTOs.stream()
//                .map(CartItemDTO::getSubTotal)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        DeliveryInfo deliveryInfo = pricingService.calculateDeliveryInfo(
//                branch, userLat, userLon
//        );
//        BigDecimal deliveryFee = deliveryInfo.getDeliveryFee();
//        BigDecimal discount = BigDecimal.ZERO;
//
//        BigDecimal total = subtotal.add(deliveryFee).subtract(discount);
//        return CheckoutCartDto.builder()
//                .id(cart.getId())
//                .restaurantBranchId(branch.getId())
//                .restaurantBranchName(branch.getRestaurant().getName())
//                .items(itemDTOs)
//                .subtotal(subtotal)
//                .promoCode(cart.getPromoCode())
//                .deliveryFee(deliveryFee)
//                .discount(discount)
//                .total(total)
//                .build();
//    }
}