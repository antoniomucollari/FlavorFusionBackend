package com.toni.FoodApp.payment.repository;

import com.toni.FoodApp.payment.entity.PaymentMethodEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethodEntity, Long> {

    @Query("""
   select pm from RestaurantBranch b
   join b.paymentMethods pm
   where b.id = :branchId
   order by pm.id
""")
    List<PaymentMethodEntity> findPaymentMethodsByBranchIdOrderById(Long branchId);

}
