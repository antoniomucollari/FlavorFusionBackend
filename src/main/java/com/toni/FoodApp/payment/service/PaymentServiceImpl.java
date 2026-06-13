package com.toni.FoodApp.payment.service;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.enums.OrderStatus;
import com.toni.FoodApp.enums.payment.PaymentMethod;
import com.toni.FoodApp.enums.payment.PaymentStatus;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.exceptions.BadRequestException;
import com.toni.FoodApp.exceptions.ForbiddenException;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.order.entity.Order;
import com.toni.FoodApp.order.mapper.OrderMapper;
import com.toni.FoodApp.order.repository.OrderRepository;
import com.toni.FoodApp.payment.Specification.PaymentSpecifications;
import com.toni.FoodApp.payment.controller.PaymentController;
import com.toni.FoodApp.payment.dtos.request.PaymentSearchCriteria;
import com.toni.FoodApp.payment.dtos.response.PaymentDTO;
import com.toni.FoodApp.payment.dtos.request.BranchPaymentUpdateRequest;
import com.toni.FoodApp.payment.dtos.response.PaymentOptionDTO;
import com.toni.FoodApp.payment.dtos.response.PokSdkOrder;
import com.toni.FoodApp.payment.entity.Payment;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.payment.mapper.PaymentMapper;
import com.toni.FoodApp.payment.repository.PaymentMethodRepository;
import com.toni.FoodApp.payment.repository.PaymentRepository;
import com.toni.FoodApp.payment.webhook.service.PokPaymentService;
import com.toni.FoodApp.response.Response;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import com.toni.FoodApp.restaurant.service.BranchService;
import com.toni.FoodApp.util.PaginationUtil;
import com.toni.FoodApp.util.RoleSpecificationHelper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService{
    private final PaymentRepository paymentRepository;
    private final ModelMapper modelMapper;
    private final UserService userService;
    private final PaymentMethodRepository paymentMethodRepository;
    private final BranchService branchService;
    private final BranchRepository branchRepository;
    private final PokPaymentService pokPaymentService;
    private final RoleSpecificationHelper roleSpecificationHelper;
    private final PaymentMapper paymentMapper;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public PaymentMethodEntity getPaymentMethod(Long id) {
        return paymentMethodRepository.findById(id).orElseThrow(()-> new EntityNotFoundException("Payment method not found with id " + id));
    }

    @Override
    public Response<List<PaymentOptionDTO>> getAllMethods() {
        List<PaymentMethodEntity> paymentMethodEntities = paymentMethodRepository.findAll();

        //map to paymentOptionDto
        List<PaymentOptionDTO> paymentDto = paymentMethodEntities.stream()
                .map(entity -> modelMapper.map(entity, PaymentOptionDTO.class))
                .collect(Collectors.toList());

        return Response.<List<PaymentOptionDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("PaymentMethods successfully loaded.")
                .data(paymentDto)
                .build();
    }

    @Override
    public Response<?> editPaymentMethod(Long paymentMethodId, PaymentOptionDTO request) {
        PaymentMethodEntity paymentMethodEntity = getPaymentMethod(paymentMethodId);
        paymentMethodEntity.setName(request.getName());
        paymentMethodRepository.save(paymentMethodEntity);
        return Response.<List<PaymentOptionDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("PaymentMethods successfully edited.")
                .build();
    }

    public Response<List<PaymentOptionDTO>> getBranchPaymentConfiguration() {
        RestaurantBranch branch = branchService.getBranchFromCurrentUser();

        List<PaymentMethodEntity> allSystemMethods = paymentMethodRepository.findAll();

        Set<Long> branchMethodIds = branch.getPaymentMethods().stream()
                .map(PaymentMethodEntity::getId)
                .collect(Collectors.toSet());

        List<PaymentOptionDTO> configList = allSystemMethods.stream()
                .map(entity -> PaymentOptionDTO.builder()
                        .id(entity.getId())
                        .name(entity.getName())
                        .paymentMethod(entity.getPaymentMethod())
                        .enabled(branchMethodIds.contains(entity.getId()))
                        .build())
                .collect(Collectors.toList());

        return Response.<List<PaymentOptionDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch payment configuration loaded.")
                .data(configList)
                .build();
    }

    @Transactional
    public Response<String> updateBranchPaymentMethods(BranchPaymentUpdateRequest request) {
        RestaurantBranch branch = branchService.getBranchFromCurrentUser();
        //empty
        if (request.getPaymentMethodIds() == null || request.getPaymentMethodIds().isEmpty()) {
            branch.getPaymentMethods().clear();
            branchRepository.save(branch);

            return Response.<String>builder()
                    .statusCode(HttpStatus.OK.value())
                    .message("All payment methods removed.")
                    .build();
        }
        List<PaymentMethodEntity> validMethods = paymentMethodRepository.findAllById(request.getPaymentMethodIds());

        //  if user sent a fake ID
        if (validMethods.size() != request.getPaymentMethodIds().size()) {
            throw new RuntimeException("One or more invalid Payment Method IDs provided.");
        }
        branch.setPaymentMethods(new HashSet<>(validMethods));

        branchRepository.save(branch);

        return Response.<String>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Branch payment methods updated successfully.")
                .build();
    }

    @Override
    public Response<PaymentController.PaymentStatusResponse> checkPaymentSuccessful(String transactionId) {
        User currentUser = userService.getCurrentLoggedInUser();
        //make sure the payment belongs to this customer
        Payment payment = paymentRepository.findByTransactionId(transactionId).orElseThrow(()-> new NotFoundException("Payment not found with id " + transactionId));
        Long ownerId = payment.getOrder().getUser().getId();
        if(currentUser.getId() != null && !currentUser.getId().equals(ownerId)) throw new ForbiddenException("You are not authorized to check this payment.");
        PaymentStatus status = payment.getPaymentStatus();
        PaymentController.PaymentStatusResponse response = new PaymentController.PaymentStatusResponse(transactionId, status);

        return Response.<PaymentController.PaymentStatusResponse>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Status of payment returned successfully.")
                .data(response)
                .build();

    }

    @Override
    public Response<PokSdkOrder> refundPayment(Long paymentId, String reason) {
        Specification<Payment> spec = Specification.allOf(PaymentSpecifications.hasPaymentId(paymentId));
        User currentUser = userService.getCurrentLoggedInUser();
        spec = roleSpecificationHelper.applyRoleBasedSecurity(
                currentUser,
                spec,
                PaymentSpecifications::hasRestaurantId,
                PaymentSpecifications::hasBranchId,
                id -> { throw new ForbiddenException("Delivery drivers are not authorized to do this."); });
        Payment payment = paymentRepository.findAll(spec).getFirst();
        if(payment == null) throw new NotFoundException("Payment not found with id " + paymentId);
        PokSdkOrder pokSdkOrder = pokPaymentService.refundPayment(payment.getTransactionId(), null, reason);
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        payment.getOrder().setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        return Response.<PokSdkOrder>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Returned payment successfully.")
                .data(pokSdkOrder)
                .build();
    }

    @Override
    public Response<Page<PaymentDTO>> getAllPayments(PaymentSearchCriteria paymentSearchCriteria) {
        User currentUser = userService.getCurrentLoggedInUser();
        Map<String, String> paymentSortMapping = Map.of(
                "lastUpdated", "lastUpdated",
                "amount", "amount",
                "order_created_date", "createdDate", //case "order_created_date" -> Sort.by(direction, "orderDate");
                "deliveryDate", "paymentDate"      
        );
        Sort sort = PaginationUtil.buildSort(paymentSearchCriteria.getSortBy(), paymentSearchCriteria.getSortDirection(), paymentSortMapping, "id");
        Pageable pageable = PageRequest.of(paymentSearchCriteria.getPage(), paymentSearchCriteria.getSize(), sort);
        Specification<Payment> spec = Specification.allOf(
                PaymentSpecifications.hasOrderId(paymentSearchCriteria.getOrderId()),
                PaymentSpecifications.hasTransactionId(paymentSearchCriteria.getTransactionId()),
                PaymentSpecifications.hasPaymentId(paymentSearchCriteria.getPaymentId()),
                PaymentSpecifications.hasPaymentStatus(paymentSearchCriteria.getPaymentStatus()),
                PaymentSpecifications.hasCustomerId(paymentSearchCriteria.getCustomerId())
        );
        boolean isCustomer = roleSpecificationHelper.isUserThisRole(currentUser, RoleName.CUSTOMER);
        if (isCustomer) {
            spec = spec.and(PaymentSpecifications.hasCustomerId(currentUser.getId()));
        } else {
            spec = roleSpecificationHelper.applyRoleBasedSecurity(
                    currentUser,
                    spec,
                    PaymentSpecifications::hasRestaurantId,
                    PaymentSpecifications::hasBranchId,
                    PaymentSpecifications::hasDeliveryId
            );
        }

        Page<Payment> paymentPage = paymentRepository.findAll(spec, pageable);
        Page<PaymentDTO> paymentDTOPage = paymentPage.map(paymentMapper::mapToPaymentDto);
        return Response.<Page<PaymentDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Returned payment successfully.")
                .data(paymentDTOPage)
                .build();
    }

    @Override
    public Response<OrderDTO> askForRefund(Long orderId) {
        User currentUser = userService.getCurrentLoggedInUser();
        Order order = orderRepository.findById(orderId).orElseThrow(()-> new NotFoundException("Order not found with id " + orderId));
        if(order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY) throw new BadRequestException("You can't ask for refund for this order.");
        if(order.getOrderStatus() != OrderStatus.CANCELLED) throw new BadRequestException("You can't ask for refund if the order is not cancelled.");
        roleSpecificationHelper.validateOrderBelongToUser(currentUser.getId(), order);
        order.setPaymentStatus(PaymentStatus.TO_REFUND);
        Payment payment = getCompletedPayment(order);
        payment.setPaymentStatus(PaymentStatus.TO_REFUND);
        orderRepository.save(order);
        OrderDTO orderDTO = orderMapper.mapToOrderDTO(order, false, true, null);

        return Response.<OrderDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Refund request sent successfully.")
                .data(orderDTO)
                .build();
    }

    private Payment getCompletedPayment(Order order){
        return order.getPayment().stream().filter(p -> p.getPaymentStatus() == PaymentStatus.COMPLETED).findFirst().orElseThrow(()-> new BadRequestException("Order has no successful payment."));
    }

}
