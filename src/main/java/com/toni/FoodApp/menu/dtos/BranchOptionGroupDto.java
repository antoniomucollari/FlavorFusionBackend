package com.toni.FoodApp.menu.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BranchOptionGroupDto {
    private Long id;
    private String name;
    private int minSelection;
    private int maxSelection;
    private List<BranchOptionVariantDto> variants;
}
