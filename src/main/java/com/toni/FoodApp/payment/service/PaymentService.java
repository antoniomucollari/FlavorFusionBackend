package com.toni.FoodApp.payment.service;

import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.payment.controller.PaymentController;
import com.toni.FoodApp.payment.dtos.request.PaymentSearchCriteria;
import com.toni.FoodApp.payment.dtos.response.PaymentDTO;
import com.toni.FoodApp.payment.dtos.request.BranchPaymentUpdateRequest;
import com.toni.FoodApp.payment.dtos.response.PaymentOptionDTO;
import com.toni.FoodApp.payment.dtos.response.PokSdkOrder;
import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import com.toni.FoodApp.response.Response;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PaymentService {
    PaymentMethodEntity getPaymentMethod(Long id);

    Response<List<PaymentOptionDTO>> getAllMethods();

    Response<?> editPaymentMethod(Long paymentMethodId, PaymentOptionDTO request);

    Response<List<PaymentOptionDTO>> getBranchPaymentConfiguration();


    Response<?> updateBranchPaymentMethods(BranchPaymentUpdateRequest request);

    Response<PaymentController.PaymentStatusResponse> checkPaymentSuccessful(String transactionId);

    Response<PokSdkOrder> refundPayment(Long paymentId, String reason);

    Response<Page<PaymentDTO>> getAllPayments(PaymentSearchCriteria paymentSearchCriteria);

    Response<OrderDTO> askForRefund(Long paymentId);
}
