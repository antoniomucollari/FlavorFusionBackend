package com.toni.FoodApp.deliverTo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toni.FoodApp.auth_users.dtos.UserDTO;
import com.toni.FoodApp.auth_users.services.UserService;
import com.toni.FoodApp.deliverTo.dtos.DeliveryLocationDTO;
import com.toni.FoodApp.deliverTo.dtos.LocationPayload;
import com.toni.FoodApp.deliverTo.services.DeliveryLocationService;
import com.toni.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.security.Principal;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/delivery-location")
@RequiredArgsConstructor
@Slf4j
public class DeliveryLocationController {
    private final DeliveryLocationService deliveryLocationService;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    @PostMapping("/deliverTo")
    public ResponseEntity<Response<?>> deliverTo(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam String locationName,
            @RequestParam(required = false) BigInteger prevLocationId,
            @RequestParam(required = false) String nickname) {
        return ResponseEntity.ok(deliveryLocationService.deliverTo(latitude, longitude, locationName,prevLocationId,nickname));
    }

    @DeleteMapping("/deliveryLocation/{id}")
    public ResponseEntity<Response<?>> deleteDeliveryLocation(@PathVariable Long id) {
        return ResponseEntity.ok(deliveryLocationService.deleteDeliveryLocation(id));
    }

    @GetMapping("/deliveryLocation")
    public ResponseEntity<Response<DeliveryLocationDTO>> deliveryLocation() {
        return ResponseEntity.ok(deliveryLocationService.deliveryActiveLocation());
    }


    @GetMapping("/all-delivery-locations")
    public ResponseEntity<Response<List<DeliveryLocationDTO>>> allDeliveryLocations() {
        return ResponseEntity.ok(deliveryLocationService.allDeliveryForUser());
    }

    @PostMapping("/location")
    public ResponseEntity<Void> updateLocation(@RequestBody LocationPayload payload) {
        try {

            Long driverId = userService.getCurrentLoggedInUser().getId();
            log.info("coordinates sent for driverId {}", driverId); 

            String jsonPayload = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(
                    "driver_loc:" + driverId,
                    jsonPayload,
                    Duration.ofHours(1)
            );
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
