package com.toni.FoodApp.cart.services;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.cart.dtos.CheckoutResponseDto;
import com.toni.FoodApp.cart.dtos.OrderQuote;
import com.toni.FoodApp.cart.dtos.OrderSummary;
import com.toni.FoodApp.cart.dtos.ResponseCartDto;
import com.toni.FoodApp.cart.entity.Cart;
import com.toni.FoodApp.cart.mapper.CheckoutMapper;
import com.toni.FoodApp.deliverTo.dtos.DeliveryInfo;
import com.toni.FoodApp.exceptions.*;
import com.toni.FoodApp.location.dto.RoutingProvider;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.payment.service.PaymentService;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.service.BranchService;
import com.toni.FoodApp.restaurant.service.PricingService;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CheckoutServiceImpl implements CheckoutService {
    private final CartService cartService;
    private final PricingService pricingService;
    private final UserService userService;
    private final BranchService branchService;
    private final CheckoutMapper checkoutMapper;
    private final PaymentService paymentService;
    @Value("${checkout.tip.suggestions}")
    private List<BigDecimal> tipSuggestions;

    @Override
    public Response<CheckoutResponseDto> generateCheckoutSummary(Long branchId) {
        // 1. Get all neccecery data
        User currentUser = getLoggedInUser();
        Cart cart = cartService.getCartForCheckout(branchId, currentUser.getId());
        PaymentMethodEntity resolvedMethod = resolveSelectedPaymentMethod(cart, branchId);
        cart.setSelectedPaymentMethod(resolvedMethod);
        OrderQuote quote = buildOrderQuote(cart);
        if(quote.getDeliveryInfo().getDistanceInKm() - 1 > cart.getRestaurantBranch().getDeliveryRadiusInKm())
            throw new AddressOutOfZoneException("You can't continue to checkout because the actual delivery route is larger then the delivery radius,even after applying the 1 km tolerance.");

        CheckoutMapper.CheckoutSummaryContext context = new CheckoutMapper.CheckoutSummaryContext(
                cart,
                quote,
                tipSuggestions,
                branchId
        );
        CheckoutResponseDto dto = checkoutMapper.toDto(context);

        return Response.<CheckoutResponseDto>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Checkout summary retrieved successfully.")
                .data(dto)
                .build();
    }

    @Override
    public Response<CheckoutResponseDto> incrementItem(Long cartItemId) {
        Response<ResponseCartDto> response = cartService.incrementItem(cartItemId);
        return generateCheckoutSummary(response.getData().getRestaurantBranchId());
    }

    @Override
    public Response<CheckoutResponseDto> decrementItem(Long cartItemId) {
        Response<ResponseCartDto> response = cartService.decrementItem(cartItemId);
        return generateCheckoutSummary(response.getData().getRestaurantBranchId());
    }

    public Response<CheckoutResponseDto> updateCartPaymentMethod(Long newPaymentMethodId, Long branchId) {
        User currentUser = getLoggedInUser();
        //get cart and validate if the user has that cart
        Cart cart =  cartService.findCartByUserIdAndBranchId(currentUser.getId(),branchId).orElseThrow(()-> new CartNotFoundException("Cart not found for user id " +  currentUser.getId() + " and restaurant venue id " + branchId));
        // Find the payment method the user WANTS to select
        PaymentMethodEntity selectedMethod = paymentService.getPaymentMethod(newPaymentMethodId);

        // Validate if the Restaurant branch can  use the new Payment Method
        if(resolveSupportedPaymentMethod(branchId, selectedMethod) != selectedMethod){
            throw new ValidationException(
                    "The selected payment method: " + selectedMethod.getName() + " is not accepted by this restaurant."
            );
        }
        //save the new method
        cartService.updateSelectedPaymentMethodAndSaveTheCart(cart, selectedMethod);
        userService.updateSelectedPaymentMethodAndSaveTheUser(currentUser, selectedMethod);

        // 6. Regenerate and return the checkout summary
        return generateCheckoutSummary(branchId);
    }

    @Override
    public Response<CheckoutResponseDto> updateTip(BigDecimal amount, Long cartId) {
        Cart cart = cartService.getCartAndValidate(cartId);
        //MAKE SURE TIP IS GREATER THAN 0
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Tip amount cannot be negative.");
        }
        //make sure the tip is different from the previous tip
        if (amount.compareTo(cart.getTipAmount()) == 0) {
            throw new BadRequestException("New tip amount must be different from the current tip amount.");
        }
        cart.setTipAmount(amount);
        cartService.save(cart);
        return generateCheckoutSummary(cart.getRestaurantBranch().getId());
    }

    @Override
    public Response<CheckoutResponseDto> updateDeliveryNote(String note, Long cartId) {
        //make sure the delivery-note is not empty or null
//        if(note.isEmpty()){
//            throw new BadRequestException("Delivery note cannot be empty.");
//        }
        Cart cart = cartService.getCartAndValidate(cartId);
        cart.setDeliveryNote(note);
        cartService.save(cart);
        return generateCheckoutSummary(cart.getRestaurantBranch().getId());
    }

    private BigDecimal calculateServiceFee(BigDecimal subtotal){
        BigDecimal percentageRate = new BigDecimal("0.05"); // 5%
        BigDecimal maxFee = new BigDecimal("150");
        BigDecimal calculatedFee = subtotal.multiply(percentageRate)
                .setScale(2, RoundingMode.HALF_UP);
        return calculatedFee.min(maxFee);

    }

    private void validateCheckoutPreconditions(Cart cart, BigDecimal subtotal) {
        validateRestaurantOpen(cart.getRestaurantBranch());
        validateMinimumOrder(cart.getRestaurantBranch().getId(), subtotal);
    }

    private void validateRestaurantOpen(RestaurantBranch branch) {
        if (branch.isClosed()) {
            String restaurantName = branch.getRestaurant().getName();
            throw new RestaurantClosedException("The restaurant: " + restaurantName + " " + branch.getAddress() + " you requested is not open at the moment.");
        }
    }

    private void validateDeliveryZone(DeliveryInfo deliveryInfo) {
        if (!deliveryInfo.isDeliverable()) {
            throw new AddressOutOfZoneException("This restaurant venue does not delivery to your address.");
        }
    }

    private void validateMinimumOrder(Long branchId, BigDecimal subtotal) {
        if (!branchService.subtotalIsHigherThanMinOrderAmount(branchId, subtotal)) {
            throw new MinimumOrderNotMetException("You have to buy more items to continue with the checkout.");
        }
    }

    public PaymentMethodEntity resolveSupportedPaymentMethod(Long branchId, PaymentMethodEntity selectedMethod){
        RestaurantBranch branch = branchService.getBranchEntityById(branchId);
        Set<PaymentMethodEntity> availableMethods = branch.getPaymentMethods();
        if(availableMethods.contains(selectedMethod))
            return selectedMethod;

        return branch.getPaymentMethods().stream()
                .findFirst()
                .orElseThrow(() -> new UnsupportedPaymentMethodException("Branch has no supported payment methods"));
    }

    private PaymentMethodEntity resolveSelectedPaymentMethod(Cart cart, Long branchId) {
        PaymentMethodEntity selectedMethodEntity = cart.getSelectedPaymentMethod();
        if(selectedMethodEntity == null){
            PaymentMethodEntity userPaymentMethodEntity = userService.getCurrentLoggedInUser().getLastSelectedPaymentMethod();
            selectedMethodEntity = resolveSupportedPaymentMethod(branchId, userPaymentMethodEntity);


        } return selectedMethodEntity;
    }

    private User getLoggedInUser() {
        return userService.getCurrentLoggedInUser();
    }

    private OrderSummary buildOrderSummary(Cart cart) {
        User currentUser = getLoggedInUser();

        BigDecimal subtotal = cartService.calculateSubtotal(cart);
        BigDecimal serviceFee = calculateServiceFee(subtotal);
        BigDecimal tipAmount = cart.getTipAmount();
        DeliveryInfo sourceDeliveryInfo = pricingService.calculateDeliveryInfo(
                cart.getRestaurantBranch(),
                currentUser.getDeliveryLocation().getLatitude(),
                currentUser.getDeliveryLocation().getLongitude(),
                RoutingProvider.HIGH_ACCURACY
        );
        BigDecimal total = subtotal
                .add(serviceFee)
                .add(tipAmount)
                .add(sourceDeliveryInfo.getDeliveryFee());

        validateCheckoutPreconditions(cart, subtotal);

        return OrderSummary.builder()
                .subtotal(subtotal)
                .serviceFee(serviceFee)
                .tipAmount(tipAmount)
                .totalAmount(total)
                .build();
    }
    @Override
    public OrderQuote buildOrderQuote(Cart cart) {
        User currentUser = getLoggedInUser();
        DeliveryInfo summaryDeliveryInfo = pricingService.calculateDeliveryInfo(cart.getRestaurantBranch(), currentUser.getDeliveryLocation().getLatitude(), currentUser.getDeliveryLocation().getLongitude(), RoutingProvider.HIGH_ACCURACY);
        validateDeliveryZone(summaryDeliveryInfo);
        OrderSummary orderSummary = buildOrderSummary(cart);
        orderSummary.setDeliveryFee(summaryDeliveryInfo.getDeliveryFee());
        return OrderQuote.builder()
                .deliveryInfo(summaryDeliveryInfo)
                .orderSummary(orderSummary)
                .build();
    }
}
