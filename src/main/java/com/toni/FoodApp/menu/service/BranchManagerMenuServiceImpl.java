package com.toni.FoodApp.menu.service;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.category.dtos.CategoryDTO;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.menu.controller.BranchManagerMenuController;
import com.toni.FoodApp.menu.dtos.BranchManagerMenu;
import com.toni.FoodApp.menu.dtos.CreateBranchManagerMenu;
import com.toni.FoodApp.menu.dtos.MenuDTO;
import com.toni.FoodApp.menu.dtos.SimpleMenuDto;
import com.toni.FoodApp.menu.entity.Menu;
import com.toni.FoodApp.menu.repository.MenuRepository;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.entity.Restaurant;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchMenuItemRepository;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchManagerMenuServiceImpl {
    private final UserService userService;
    private final BranchRepository branchRepository;
    private final BranchMenuItemRepository branchMenuItemRepository;
    private final MenuRepository menuRepository;
    private final CachedMenuService cachedMenuService;

    public Response<Page<BranchManagerMenu>> getMenus(Pageable pageable) {
        //GET THE BRANCH ID
        RestaurantBranch restaurantBranch = getRestaurantBranch();
        if (pageable == null) {
            pageable = PageRequest.of(0, 10);
        }
        //get menus
        Page<BranchMenuItem> branchMenuItem = branchMenuItemRepository.findAllByBranchId(restaurantBranch.getId(), pageable);

        //map to dto
        Page<BranchManagerMenu> dto = branchMenuItem
                .map(item -> BranchManagerMenu.builder()
                        .id(item.getId())
                        .name(item.getMenu().getName())
                        .imageUrl(item.getMenu().getImageUrl())
                        .price(item.getPrice())
                        .isHighlighted(item.isHighlighted())
                        .categoryName(item.getMenu().getCategory().getName())
                        .isAvailable(item.isAvailable())
                        .build());

        return Response.<Page<BranchManagerMenu>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Menus retrieved successfully")
                .data(dto)
                .build();
    }


    public Response<?> createMenu(CreateBranchManagerMenu createBranchManagerMenu) {
        //todo security check
        Menu menu = menuRepository.findById(createBranchManagerMenu.getMenuId()).orElseThrow(() -> new RuntimeException("Menu not found"));
        RestaurantBranch restaurantBranch = getRestaurantBranch();
        
        BranchMenuItem branchMenuItem = BranchMenuItem.builder()
                .price(createBranchManagerMenu.getPrice())
                .menu(menu)
                .branch(restaurantBranch)
                .isHighlighted(createBranchManagerMenu.isHighlighted())
                .isAvailable(createBranchManagerMenu.isAvailable())
                .build();
        branchMenuItemRepository.save(branchMenuItem);
        cachedMenuService.clearMenuCache(restaurantBranch.getId());
        return Response.<Page<BranchManagerMenu>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch Menu created successfully!")
                .build();
    }

    public Response<List<SimpleMenuDto>> getRestaurantMenus() {
        RestaurantBranch restaurantBranch = getRestaurantBranch();
        Restaurant restaurant = restaurantBranch.getRestaurant();
        List<Menu> menus = menuRepository.findMenusNotLinkedToBranch(restaurant.getId(), restaurantBranch.getId());
        List<SimpleMenuDto> simpleMenuDtos = menus.stream().map(menu -> SimpleMenuDto.builder().id(menu.getId()).name(menu.getName()).build()).toList();
        return Response.<List<SimpleMenuDto>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Restaurant Menus retrieved successfully!")
                .data(simpleMenuDtos)
                .build();
    }


    private RestaurantBranch getRestaurantBranch(){
        User currentUser = userService.getCurrentLoggedInUser();
        return branchRepository.findByManagerId(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("No branch found for this user"));
    }

    public Response<?> updateMenu(BranchManagerMenuController.BranchMenuDto branchMenuDto) {
        //get the branch menu item
        BranchMenuItem branchMenuItem = branchMenuItemRepository.findById(branchMenuDto.id()).orElseThrow();
        int changesCount = 0;
        if(branchMenuItem.isAvailable() != branchMenuDto.available()){
            branchMenuItem.setAvailable(branchMenuDto.available());
            changesCount++;
        }
        if(branchMenuItem.isHighlighted() != branchMenuDto.highlighted()){
            branchMenuItem.setHighlighted(branchMenuDto.highlighted());
            changesCount++;
        }
        if (!branchMenuItem.getPrice().equals(branchMenuDto.price())){
            branchMenuItem.setPrice(branchMenuDto.price());
            changesCount++;
        }
        if(changesCount == 0)
            throw new BadRequestException("Given values were as previous. No update was made.");
        branchMenuItemRepository.save(branchMenuItem);
        Long branchId = branchMenuItem.getBranch().getId();
        cachedMenuService.clearMenuCache(branchId);
        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch Menu Item updated successfully. Changes count: " + changesCount)
                .build();
    }
}
