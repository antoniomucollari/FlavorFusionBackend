package com.toni.FoodApp.cart.controller;


import com.toni.FoodApp.cart.dtos.AddItemToCartRequest;
//import com.toni.FoodApp.cart.service.CartService;
import com.toni.FoodApp.cart.dtos.ResponseCartDto;
import com.toni.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor //only works when there are final fields
public class CartController {

    private final com.toni.FoodApp.cart.services.CartService cartService;

    @PostMapping("/basket")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<?>> addToCart(@RequestBody AddItemToCartRequest cartDTO){
        return  ResponseEntity.ok(cartService.addItemToCart(cartDTO));
    }

    @GetMapping("/basket/{branchId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<ResponseCartDto>> getBasketItems(@PathVariable Long branchId){
        return ResponseEntity.ok(cartService.getShoppingCart(branchId));
    }

    @PostMapping("/basket/increment/{cartItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<ResponseCartDto>> incrementItem(@PathVariable Long cartItemId){
        return  ResponseEntity.ok(cartService.incrementItem(cartItemId));
    }

    @PostMapping("/basket/decrement/{cartItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<ResponseCartDto>> decrementItem(@PathVariable Long cartItemId){
        return  ResponseEntity.ok(cartService.decrementItem(cartItemId));
    }

    @DeleteMapping("/basket/remove/{cartItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<ResponseCartDto>> removeItem(@PathVariable Long cartItemId){
        return  ResponseEntity.ok(cartService.removeItem(cartItemId));
    }

    @DeleteMapping("/basket/clear/{branchId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<?>> clearShoppingCart(@PathVariable Long branchId){
        return ResponseEntity.ok(cartService.clearShoppingCart(branchId, null));
    }

    @PutMapping("/orderAgain/{orderId}")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<Response<Long>> orderAgain(@PathVariable Long orderId){
        return ResponseEntity.ok(cartService.orderAgain(orderId));
    }
}
