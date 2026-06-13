package com.toni.FoodApp.cart.mapper;

import com.toni.FoodApp.cart.dtos.CartItemDTO;
import com.toni.FoodApp.cart.dtos.VariantAvailabilityDTO;
import com.toni.FoodApp.cart.entity.CartItem;
import com.toni.FoodApp.cart.entity.CartItemVariant;
import com.toni.FoodApp.menu.dtos.VariantDTO;
import com.toni.FoodApp.menu.entity.Menu;
import com.toni.FoodApp.menu.helper.HelperMenuOptionAndVariantService;
import com.toni.FoodApp.menu.mapper.MenuMapper;
import com.toni.FoodApp.menu.repository.OptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

    @Component
    @RequiredArgsConstructor
public class CartMapper {
    private final MenuMapper menuMapper;
    private final OptionRepository optionRepository;
    private final HelperMenuOptionAndVariantService helperMenuOptionAndVariantService;


    // Note: No Repository injection needed anymore!

    public CartItemDTO toDto(CartItem item, Map<Long, VariantAvailabilityDTO> availabilityMap) {
        Menu masterMenu = item.getBranchMenuItem().getMenu();

        List<String> validationErrors = new ArrayList<>();
        boolean isValid = true;

        // 1. Check Main Item Availability
        if (!item.getBranchMenuItem().isAvailable()) {
            isValid = false;
            validationErrors.add("Item is no longer available");
        }

        // 2. Check Variants Availability
        for (CartItemVariant civ : item.getCartItemVariants()) {
            Long variantId = civ.getOptionVariant().getId();

            // Get the DTO from the map
            VariantAvailabilityDTO variantInfo = availabilityMap.get(variantId);

            // Check if it exists in the map AND is marked available
            boolean isAvailable = (variantInfo != null && variantInfo.getIsAvailable());

            if (!isAvailable) {
                isValid = false;
                validationErrors.add(civ.getOptionVariant().getName() + " is out of stock.");
            }
        }

        return CartItemDTO.builder()
                .id(item.getId())
                .name(masterMenu.getName())
                .quantity(item.getQuantity())
                .pricePerUnit(item.getPricePerUnit())
                .subTotal(item.getSubTotal())
                // 3. Pass the map to the helper to handle Price Overrides
                .variants(mapToVariantFromCartItemVariant(item.getCartItemVariants(), availabilityMap))
                .isValid(isValid)
                .validationMessages(validationErrors)
                .build();
    }

    // UPDATED HELPER: Now accepts the Map to update prices during mapping
    private List<VariantDTO> mapToVariantFromCartItemVariant(
            List<CartItemVariant> cartItemVariants,
            Map<Long, VariantAvailabilityDTO> availabilityMap
    ) {
        return cartItemVariants.stream().map(cartItemVariant -> {
            // 1. Convert Entity to DTO
            VariantDTO dto = menuMapper.mapToVariantDTO(cartItemVariant.getOptionVariant());

            // 2. Check for Price Override in the Map
            VariantAvailabilityDTO info = availabilityMap.get(dto.getId());

            // If we have updated info and the price is different, update the DTO
            if (info != null && info.getPrice() != null) {
                // Use compareTo for safe BigDecimal comparison
                if (info.getPrice().compareTo(dto.getRecommendedPrice()) != 0) {
                    dto.setRecommendedPrice(info.getPrice());
                }
            }
            return dto;
        }).toList();
    }
}
