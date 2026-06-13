package com.toni.FoodApp.deliverTo.repository;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.deliverTo.entity.DeliveryLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryLocationRepository extends JpaRepository<DeliveryLocation, Long> {
    List<DeliveryLocation> findAllByUser(User user);
}
