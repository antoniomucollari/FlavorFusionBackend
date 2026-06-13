package com.toni.FoodApp.menu.dtos;

import com.toni.FoodApp.menu.entity.OptionVariant;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionGroupDTO {
    private Long id;

    @NotNull
    private String name;
    @Min(0)
    private int minSelection;
    @Min(1)
    private int maxSelection;
    private boolean isDeleted = false;
    @NotNull
    private List<VariantDTO> variants = new ArrayList<>();
}
