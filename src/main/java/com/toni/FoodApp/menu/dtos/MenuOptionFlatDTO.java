package com.toni.FoodApp.menu.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MenuOptionFlatDTO {

    //  Option Group Data
    private Long groupId;
    private String groupName;
    private Integer minSelection;
    private Integer maxSelection;

    // Variant Data
    private Long variantId;
    private String variantName;
    private BigDecimal effectivePrice;
}