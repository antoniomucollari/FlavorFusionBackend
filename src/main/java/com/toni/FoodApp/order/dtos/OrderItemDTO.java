    package com.toni.FoodApp.order.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.toni.FoodApp.menu.dtos.SimpleMenuDto;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemDTO {
        private Long id;
        private int quantity;

        private SimpleMenuDto menu;

        private BigDecimal pricePerUnit;
        private BigDecimal subTotal;

        private List<OrderItemVariantDto> variants;

}
