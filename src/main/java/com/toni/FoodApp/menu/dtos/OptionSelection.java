package com.toni.FoodApp.menu.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionSelection {
    private Long optionGroupId;
    @NotEmpty
    private List<Long> variantIds;
}
