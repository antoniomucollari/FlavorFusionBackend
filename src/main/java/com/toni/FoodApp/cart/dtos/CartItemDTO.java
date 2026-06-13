    package com.toni.FoodApp.cart.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.toni.FoodApp.menu.dtos.MenuDTO;
import com.toni.FoodApp.menu.dtos.VariantDTO;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

    @Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CartItemDTO {
    private Long id;
    private Long branchMenuItemId;
    private String name;
    private String imageUrl;
    private int quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal subTotal;
    private List<VariantDTO> variants;
    private boolean isValid;
    private List<String> validationMessages;
}
