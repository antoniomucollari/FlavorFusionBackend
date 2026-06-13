package com.toni.FoodApp.cart.dtos;

import com.toni.FoodApp.menu.dtos.OptionGroupDTO;
import com.toni.FoodApp.menu.dtos.OptionSelection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Data
public class AddItemToCartRequest {
    @NotNull
    private Long branchId;

    @NotNull
    private Long branchMenuItemId;

    @NotNull
    @Min(1)
    private int quantity; // How many

    private String specialInstructions;
    //new
    private List<OptionSelection> options;

}