package com.toni.FoodApp.menu.service;

import com.toni.FoodApp.menu.dtos.*;
import com.toni.FoodApp.response.Response;

import java.util.List;

public interface MenuService {
    Response<MenuDTO> createMenu(CreateMenuRequest menuDTO);
    Response<CreateMenuRequest> updateMenu(CreateMenuRequest menuDTO);
    Response<MenuResponse> getMenuByBranchId(Long branchMenuId);
    Response<MenuDTO> deleteMenu(Long id);
    Response<List<MenuDTO>> getMenus(Long categoryId, String searchTerm, Boolean restaurantId );

    Response<?> createMenuOption(OptionGroupDTO optionDTO, Long menuId);

    Response<List<OptionGroupDTO>> getAllOptions(Long menuId);

    Response<?> unlinkOptionFromMenu(Long menuId, Long optionId);

    Response<List<OptionGroupDTO>> getAllOptionsForRestaurant();

    Response<List<OptionGroupDTO>> getAvailableOptions(Long menuId);

    Response<?> editMenuOption(OptionGroupDTO optionDTO);

    Response<?> deleteOption(Long optionId);

    Response<?> linkOptionFromMenu(Long menuId, Long optionId);

    Response<List<BranchOptionGroupDto>> getAllOptionsBranch(Long menuId);

    Response<List<OptionGroupDTO>> getAllOptionsCustomer(Long menuId, Long branchId);

    public Response<?> overwriteMenuOptions(List<BranchConfigUpdateRequest> requests,Long optionId);
}
