package com.toni.FoodApp.menu.controller;

import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.menu.dtos.*;
import com.toni.FoodApp.menu.service.MenuService;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.repository.BranchMenuItemRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Stack;

@RestController
@RequestMapping({"/api/menu", "/api/menu/"})
@RequiredArgsConstructor
public class MenuController {
    private final BranchMenuItemRepository branchMenuItemRepository;
    private final MenuService menuService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<MenuDTO>> createMenu(
            @ModelAttribute CreateMenuRequest menuDTO,
            @RequestParam(value = "imageFile", required = true) MultipartFile imageFile
    ){
        menuDTO.setImageFile(imageFile);
        return ResponseEntity.ok(menuService.createMenu(menuDTO));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<CreateMenuRequest>> updateMenu(
            @ModelAttribute CreateMenuRequest menuDTO,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile
    ){
        menuDTO.setImageFile(imageFile);
        return ResponseEntity.ok(menuService.updateMenu(menuDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response<MenuResponse>> getMenuById(@PathVariable Long id){
        return ResponseEntity.ok(menuService.getMenuByBranchId(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<MenuDTO>> deleteMenu(@PathVariable Long id){
        return ResponseEntity.ok(menuService.deleteMenu(id));
    }

    @GetMapping()
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<List<MenuDTO>>> getMenus(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Boolean myMenus) {
        return ResponseEntity.ok(menuService.getMenus(categoryId,searchTerm, myMenus));
    }


    @PostMapping("/option")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> createOptionAndVariants(
            @RequestParam Long menuId,
            @RequestBody OptionGroupDTO optionDTO
    ){
        return ResponseEntity.ok(menuService.createMenuOption(optionDTO, menuId));
    }

    @PutMapping("/option")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> editOptionAndVariants(
            @Valid @RequestBody OptionGroupDTO optionDTO
    ){
        return ResponseEntity.ok(menuService.editMenuOption(optionDTO));
    }


    @GetMapping("/options-restaurant")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<List<OptionGroupDTO>>> getAllOptionsRestaurant(
            @RequestParam Long menuId
    ){
        return ResponseEntity.ok(menuService.getAllOptions(menuId));
    }

    @GetMapping("/options-customer/{branchMenuId}/{branchId}")
    public ResponseEntity<Response<List<OptionGroupDTO>>> getAllOptionsCustomer(
            @PathVariable Long branchMenuId,
            @PathVariable Long branchId
    ){
        return ResponseEntity.ok(menuService.getAllOptionsCustomer(branchMenuId, branchId));
    }


    @GetMapping("/options-branch-manager")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<List<BranchOptionGroupDto>>> getAllOptionsBranch(
            @RequestParam Long branchMenuId
    ){
        Long menuId = branchMenuItemRepository.findById(branchMenuId)
                .map(item -> item.getMenu().getId())
                .orElseThrow(() -> new NotFoundException("Branch menu item or menu not found"));
        return ResponseEntity.ok(menuService.getAllOptionsBranch(menuId));
    }

    //unlinkOptionFromMenu
    @PutMapping("/unlink")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> unlinkOptionFromMenu(@RequestParam Long menuId,
                                                            @RequestParam Long optionId
    ){
        return ResponseEntity.ok(menuService.unlinkOptionFromMenu(menuId, optionId));
    }


    @PutMapping("/link")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> linkOptionFromMenu(@RequestParam Long menuId, @RequestParam Long optionId){
        return ResponseEntity.ok(menuService.linkOptionFromMenu(menuId, optionId));
    }


    @GetMapping("/allOptions")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<List<OptionGroupDTO>>> getAllOptionsForRestaurant(){
        return ResponseEntity.ok(menuService.getAllOptionsForRestaurant());
    }

    @GetMapping("/available-options")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<Response<List<OptionGroupDTO>>> getAvailableOptions(
            @RequestParam Long menuId
    ){
        return ResponseEntity.ok(menuService.getAvailableOptions(menuId));
    }

    @DeleteMapping("/options")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')or hasAuthority('MANAGER')")
    public ResponseEntity<Response<?>> deleteOption(@RequestParam Long optionId){
        return ResponseEntity.ok(menuService.deleteOption(optionId));
    }

    @PutMapping("/option-branch")
    @PreAuthorize("hasAuthority('BRANCH_MANAGER')")
    public ResponseEntity<Response<?>> overwriteOptionAndVariants(
            @RequestParam Long optionId,
            @RequestBody List<BranchConfigUpdateRequest> updates
    ){
        return ResponseEntity.ok(menuService.overwriteMenuOptions(updates, optionId));
    }
    

}
