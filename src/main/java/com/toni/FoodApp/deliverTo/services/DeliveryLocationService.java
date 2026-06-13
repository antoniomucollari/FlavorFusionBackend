package com.toni.FoodApp.deliverTo.services;

import com.toni.FoodApp.deliverTo.dtos.DeliveryLocationDTO;
import com.toni.FoodApp.response.Response;

import java.math.BigInteger;
import java.util.List;

public interface DeliveryLocationService{
    Response<?> deliverTo(Double latitude, Double longitude, String locationName, BigInteger prevLocationId,String nickname);

    Response<DeliveryLocationDTO> deliveryActiveLocation();

    Response<List<DeliveryLocationDTO>> allDeliveryForUser();

    Response<?> deleteDeliveryLocation(Long id);
}
