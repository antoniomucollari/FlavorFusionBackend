package com.toni.FoodApp.menu.helper;

import com.toni.FoodApp.menu.dtos.MenuOptionFlatDTO;
import com.toni.FoodApp.menu.dtos.VariantDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class HelperMenuOptionAndVariantService {
    /*DONT NEED IT
    public List<VariantDTO> extractEffectivePriceFromVariants(List<VariantDTO> chosenMenuVariants, List<MenuOptionFlatDTO> allPossibleVariants) {
        Map<Long, BigDecimal> effectivePriceMap = allPossibleVariants.stream()
                .collect(Collectors.toMap(
                        MenuOptionFlatDTO::getVariantId,
                        MenuOptionFlatDTO::getEffectivePrice,
                        (existing, replacement) -> existing
                ));

        return chosenMenuVariants.stream()
                .map(variant -> {
                    BigDecimal effectivePrice = effectivePriceMap.get(variant.getId());
                    if (effectivePrice != null && effectivePrice.compareTo(variant.getRecommendedPrice()) != 0) {
                        variant.setRecommendedPrice(effectivePrice);
                    }
                    return variant;
                })
                .toList();
    }

     */
}
