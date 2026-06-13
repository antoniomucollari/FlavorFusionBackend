package com.toni.FoodApp.cart.services;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.cart.dtos.AddItemToCartRequest;
import com.toni.FoodApp.cart.dtos.ResponseCartDto;
import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.cart.entity.CartItem;
import com.toni.FoodApp.cart.entity.CartItemVariant;
import com.toni.FoodApp.cart.repository.CartItemRepository;
import com.toni.FoodApp.cart.repository.CartItemVariantRepository;
import com.toni.FoodApp.cart.repository.CartRepository;
import com.toni.FoodApp.exceptions.*;
import com.toni.FoodApp.menu.dtos.OptionSelection;
import com.toni.FoodApp.menu.dtos.VariantWithPriceDTO;
import com.toni.FoodApp.menu.entity.OptionGroup;
import com.toni.FoodApp.menu.entity.OptionVariant;
import com.toni.FoodApp.menu.repository.OptionRepository;
import com.toni.FoodApp.menu.repository.OptionVariantRepository;
import com.toni.FoodApp.menu.validation.OptionValidator;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.order.entity.OrderItem;
import com.toni.FoodApp.order.entity.OrderItemVariant;
import com.toni.FoodApp.order.repository.OrderRepository;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.BranchMenuItem;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchMenuItemRepository;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CartServiceImpl implements CartService {
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final BranchMenuItemRepository branchMenuItemRepository;
    private final UserService userService;
    private final CartViewBuilderService cartViewBuilderService;
    private final BranchRepository branchRepository;
    private final CartItemVariantRepository cartItemVariantRepository;
    private final OptionValidator optionValidator;
    private final OptionVariantRepository optionVariantRepository;
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;

    @Override
    @Transactional
    public Response<ResponseCartDto> addItemToCart(AddItemToCartRequest cartDTO) {
        // 1. Validation
        optionValidator.validateOptionSelections(cartDTO.getOptions(), cartDTO.getBranchMenuItemId());
        BranchMenuItem itemToAdd = findAndValidateBranchMenuItem(cartDTO.getBranchMenuItemId());
        int quantity = cartDTO.getQuantity();
        User user = getLoggedInUser();
        RestaurantBranch currentBranch = itemToAdd.getBranch();

        Cart cart = getOrCreateCart(user, currentBranch);
        Set<Long> incomingVariantIds = cartDTO.getOptions().stream()
                .flatMap(os -> os.getVariantIds().stream())
                .collect(Collectors.toSet());
        List<VariantWithPriceDTO> variantData = optionVariantRepository
                .findVariantsWithEffectivePrice(incomingVariantIds, itemToAdd.getBranch().getId());

        List<OptionVariant> selectedVariants = optionVariantRepository.findAllById(incomingVariantIds);

        // (Optional: You might need to check BranchOptionConfig here for price overrides,
        // strictly depending on your previous logic about "Effective Price")
        BigDecimal variantsTotal = variantData.stream()
                .map(VariantWithPriceDTO::getEffectivePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalPricePerUnit = itemToAdd.getPrice().add(variantsTotal);

        // 4. Find an EXACT Existing Match (Product + Variants)
        Optional<CartItem> existingItemOpt = cart.getCartItems().stream()
                .filter(ci -> ci.getBranchMenuItem().getId().equals(itemToAdd.getId())) // Match Product
                .filter(ci -> {
                    // Match Variants
                    Set<Long> existingVariantIds = ci.getCartItemVariants().stream()
                            .map(civ -> civ.getOptionVariant().getId())
                            .collect(Collectors.toSet());
                    return existingVariantIds.equals(incomingVariantIds);
                })
                .findFirst();

        // 5. Update or Create
        if (existingItemOpt.isPresent()) {
            // --- SCENARIO A: Merge into existing line item ---
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + quantity);

            // Recalculate subtotal based on new quantity
            existingItem.setSubTotal(
                    existingItem.getPricePerUnit().multiply(BigDecimal.valueOf(existingItem.getQuantity()))
            );
            // Note: No need to touch CartItemVariantRepository here, the variants are already identical.

        } else {
            // --- SCENARIO B: Create new line item ---
            CartItem newCartItem = CartItem.builder()
                    .cart(cart)
                    .branchMenuItem(itemToAdd)
                    .quantity(quantity)
                    .pricePerUnit(finalPricePerUnit)
                    .subTotal(finalPricePerUnit.multiply(BigDecimal.valueOf(quantity)))
                    .build();

            // Create the Variant Joins
            List<CartItemVariant> newVariants = variantData.stream()
                    .map(dto -> CartItemVariant.builder()
                            .cartItem(newCartItem)
                            .optionVariant(dto.getVariant()) // We have the entity ready!
                            .build())
                    .collect(Collectors.toList());

            newCartItem.setCartItemVariants(newVariants);

            // Add to parent (CascadeType.ALL will save the children automatically)
            cart.getCartItems().add(newCartItem);
        }

        // 6. Save and Return
        Cart savedCart = cartRepository.save(cart);
        ResponseCartDto cartView = cartViewBuilderService.buildCartView(savedCart);

        return Response.<ResponseCartDto>builder()
                .statusCode(HttpStatus.CREATED.value())
                .message("Item added successfully")
                .data(cartView)
                .build();
    }



    @Override
    @Transactional()
    public Response<ResponseCartDto> getShoppingCart(Long branchId) {

        Cart cart = getOrCreateCartForCurrentUserAndBranch(branchId);

        ResponseCartDto cartView = cartViewBuilderService.buildCartView(cart);
        return Response.<ResponseCartDto>builder()
                .statusCode(HttpStatus.OK.value())
                .data(cartView)
                .message("Cart retrieved successfully")
                .build();
    }
    private Cart getOrCreateCart(User user, RestaurantBranch branchToAdd) {
        Optional<Cart> cartOpt = cartRepository.findByUserIdAndRestaurantBranchId(user.getId(), branchToAdd.getId());

        if (cartOpt.isEmpty()) {
            Cart newCart = new Cart();
            newCart.setUser(user);
            newCart.setRestaurantBranch(branchToAdd);
            return newCart;

        } else {
            Cart existingCart = cartOpt.get();
            if (!existingCart.getRestaurantBranch().getId().equals(branchToAdd.getId())) {
                throw new BusinessLogicException(
                        "You can only order from one restaurant at a time. Clear your cart to add this item."
                );
            }

            // --- Case 3: No conflict. Return the existing cart. ---
            return existingCart;
        }
    }


    private Response<ResponseCartDto> updateCartItemQuantity(Long cartItemId, int amount) {
        log.info("Inside updateCartItemQuantity() for cartItemId: {} with amount: {}", cartItemId, amount);

        User user = userService.getCurrentLoggedInUser();


        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        Cart cart = cartItem.getCart();

        //validate if the user can modify this cartItem
        validateUserHasCart(cart);

        int newQuantity = cartItem.getQuantity() + amount;

        if (newQuantity <= 0) {
            // Remove the item
            cart.getCartItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
            cartRepository.save(cart);


            return Response.<ResponseCartDto>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("Item removed from cart")
                    .data(cartViewBuilderService.buildCartView(cart))
                    .build();
        } else {
            cartItem.setQuantity(newQuantity);
            cartItem.setSubTotal(cartItem.getPricePerUnit().multiply(BigDecimal.valueOf(newQuantity)));
            cartItemRepository.save(cartItem);

            return Response.<ResponseCartDto>builder()
                    .statusCode(HttpStatus.OK.value())
                    .data(cartViewBuilderService.buildCartView(cart))
                    .message("Quantity updated")
                    .build();
        }
    }

    @Override
    public Response<ResponseCartDto> incrementItem(Long cartItemId) {
        return updateCartItemQuantity(cartItemId, 1);
    }

    @Override
    public Response<ResponseCartDto> decrementItem(Long cartItemId) {
        return updateCartItemQuantity(cartItemId, -1);
    }

    @Override
    @Transactional
    public Response<ResponseCartDto> removeItem(Long cartItemId) {
        log.info("Inside removeItem() for cartItemId: {}", cartItemId);

        // Find the specific cart item by its ID.
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item with id " + cartItemId + " not found"));
        Cart cart = cartItem.getCart();
        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        cartRepository.save(cart);

        ResponseCartDto updatedCartView = cartViewBuilderService.buildCartView(cart);
        return Response.<ResponseCartDto>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Item removed from cart")
                .data(updatedCartView)
                .build();
    }



    @Override
    public Response<?> clearShoppingCart(Long branchId, Long userId) {
        Long customerId = userId == null ? getLoggedInUser().getId() : userId;
        Optional<Cart> cartOptional = cartRepository.findByUserIdAndRestaurantBranchId(customerId, branchId);
        if (cartOptional.isEmpty()) {
            return Response.builder()
                    .statusCode(HttpStatus.NO_CONTENT.value())
                    .message("Cart is already empty")
                    .build();
        }
        Cart cart = cartOptional.get();
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cartRepository.save(cart);
        return Response.builder()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .message("Success")
                .build();
    }

    void deleteCartHelper(Cart cart){
        if(cart.getCartItems().isEmpty()) return;
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }

    @Override
    public BigDecimal calculateSubtotal(Cart cart) {
        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return cart.getCartItems().stream()
                .map(CartItem::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Cart getOrCreateCartForCurrentUserAndBranch(Long branchId) {
        return findCartByUserIdAndBranchId(getLoggedInUser().getId(), branchId)
                .orElseGet(() -> createNewCart(getLoggedInUser(), branchId));
    }
    private CartItem findCartItemByBranchMenuItemId(Cart cart, Long cartItemId) {
        return cart.getCartItems().stream()
                .filter(ci -> ci.getBranchMenuItem().getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found in cart"));
    }
    private Cart removeCartItemAndSave(Cart cart, CartItem cartItem) {
        cart.getCartItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        return cartRepository.save(cart);
    }

    private BranchMenuItem findAndValidateBranchMenuItem(Long cartItemId) {
        BranchMenuItem item = branchMenuItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!item.isAvailable()) {
            throw new BusinessLogicException("This item is currently not available");
        }
        return item;
    }

    @Override
    public Cart getCartForCheckout(Long branchId, Long userId) {
        Cart cart = findCartByUserIdAndBranchId(userId, branchId)
                .orElseThrow(() -> new CartNotFoundException("No active cart found for this restaurant."));

        if (cart.getCartItems().isEmpty()) {
            throw new CartIsEmptyException("Cannot proceed to checkout with an empty cart.");
        }

        return cart;
    }

    private Cart createNewCart(User user, Long branchId) {
        RestaurantBranch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Branch not found with id: " + branchId));

        Cart newCart = new Cart();
        newCart.setUser(user);
        newCart.setRestaurantBranch(branch);

        return cartRepository.save(newCart);
    }

    @Override
    public Optional<Cart> findCartByUserIdAndBranchId(Long userId, Long branchId) {
        return cartRepository.findByUserIdAndRestaurantBranchId(userId, branchId);
    }

    @Override
    public void save(Cart cart) {
        cartRepository.save(cart);
    }

    @Override
    public void updateSelectedPaymentMethodAndSaveTheCart(Cart cart, PaymentMethodEntity method) {
        cart.setSelectedPaymentMethod(method);
        save(cart);
    }

    @Override
    public Cart getCartAndValidate(Long cartId){
        Cart cart = cartRepository.findById(cartId).orElseThrow(()-> new NotFoundException("Cart not found with id: " + cartId));
        validateUserHasCart(cart);
        return cart;

    }
    @Transactional // all or nothing
    @Override
    public Response<Long> orderAgain(Long orderId) {
        // get the user
        User currentUser = getLoggedInUser();
        //get the order and validate if this order belong to the user
        Order order = orderRepository.findByIdAndUserId(orderId, currentUser.getId()).orElseThrow(()->
                new BadRequestException("The given orderId nor does not belong to user or is invalid. ") );
        //delete cart
        Long branchId = order.getBranch().getId();
        Cart cart = cartRepository.findByUserIdAndRestaurantBranchId(currentUser.getId(), branchId)
                .orElseThrow(()-> new NotFoundException("Cart not found."));
        deleteCartHelper(cart);
        cartRepository.flush();

        for (OrderItem orderItem : order.getOrderItems()) {
            AddItemToCartRequest request = mapOrderItemToRequest(orderItem, order.getBranch().getId());
            addItemToCart(request);
        }


        return Response.<Long>builder()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .message("Success. Redirecting to basket.")
                .data(branchId)
                .build();
    }

    private AddItemToCartRequest mapOrderItemToRequest(OrderItem orderItem, Long branchId) {
        AddItemToCartRequest request = new AddItemToCartRequest();
        BranchMenuItem currentBranchItem = branchMenuItemRepository.findByMenuIdAndBranchId(
                orderItem.getMenu().getId(),
                branchId
        );
        request.setBranchMenuItemId(currentBranchItem.getId());
        request.setQuantity(orderItem.getQuantity());

        Map<Long, List<Long>> variantsByOption = orderItem.getVariants().stream()
                .collect(Collectors.groupingBy(
                        OrderItemVariant::getOriginalOptionId,
                        Collectors.mapping(OrderItemVariant::getOriginalVariantId, Collectors.toList())
                ));

        List<OptionSelection> optionSelections = variantsByOption.entrySet().stream()
                .map(entry -> {
                    OptionSelection selection = new OptionSelection();
                    selection.setOptionGroupId(entry.getKey());
                    selection.setVariantIds(entry.getValue());
                    return selection;
                })
                .collect(Collectors.toList());

        request.setOptions(optionSelections);
        return request;
    }

    private void validateUserHasCart(Cart cart) {
        if (!cart.getUser().getId().equals(getLoggedInUser().getId())) {
            throw new SecurityException("User does not have permission to modify this cart item");
        }
    }

    private User getLoggedInUser() {
        return userService.getCurrentLoggedInUser();
    }

}
