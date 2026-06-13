package com.toni.FoodApp.menu.service;

import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.category.entity.Category;
import com.toni.FoodApp.menu.entity.Menu;
import com.toni.FoodApp.restaurant.dtos.MenuItemDto;
import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j

public class CachedMenuService {

    private final BranchRepository branchRepository;
    // ... inject your mappers ...

    @Cacheable(value = "branchMenus", key = "#branchId")
    public List<CategoryDTO> getFullMenu(Long branchId) {
        log.info("Cache Miss! Fetching full menu from DB for branch: {}", branchId);

        // Fetch ALL items (empty search string)
        List<BranchMenuItem> menuItems = branchRepository.findAllWithDetailsAndSearch(branchId, "");

        Map<Category, List<BranchMenuItem>> itemsByCategory = menuItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getMenu().getCategory(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return itemsByCategory.entrySet().stream()
                .map(entry -> {
                    Category category = entry.getKey();
                    List<MenuItemDto> itemDtos = entry.getValue().stream()
                            .map(this::mapToMenuItemDto)
                            .collect(Collectors.toList());

                    return CategoryDTO.builder()
                            .id(category.getId())
                            .name(category.getName())
                            .menus(itemDtos)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // We will call this when a manager updates a price or availability
    @CacheEvict(value = "branchMenus", key = "#branchId")
    public void clearMenuCache(Long branchId) {
        log.info("Menu cache cleared for branch: {}", branchId);
    }
    private MenuItemDto mapToMenuItemDto(BranchMenuItem item) {
        Menu masterItem = item.getMenu();

        return MenuItemDto.builder()
                .id(item.getId()) // Use BranchMenuItem ID for cart/order
                .name(masterItem.getName())
                .description(masterItem.getDescription())
                .imageUrl(masterItem.getImageUrl())
                .price(item.getPrice()) // Branch-specific price
                .isAvailable(item.isAvailable()) // Branch-specific availability
                .isHighlighted(item.isHighlighted()) // Branch-specific highlight
                .build();
    }
}