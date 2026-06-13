package com.toni.FoodApp.deliverTo.services;

import com.toni.FoodApp.auth_users.dtos.UserDTO;
import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.auth_users.repository.UserRepository;
import com.toni.FoodApp.auth_users.services.UserServiceImpl;
import com.toni.FoodApp.deliverTo.dtos.DeliveryLocationDTO;
import com.toni.FoodApp.deliverTo.entity.DeliveryLocation;
import com.toni.FoodApp.deliverTo.repository.DeliveryLocationRepository;
import com.toni.FoodApp.exceptions.NotFoundException;
import com.toni.FoodApp.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryLocationServiceImpl implements DeliveryLocationService {
    private final DeliveryLocationRepository deliveryLocationRepository;
//    private final UserRepository userRepository;
    private final UserServiceImpl userService;
    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    @Override
    @Transactional
    public Response<?> deliverTo(Double latitude, Double longitude, String locationName, BigInteger prevLocationId,String nickname) {
        User user = userService.getCurrentLoggedInUser();
        DeliveryLocation location;

        if (prevLocationId != null) {
            location = deliveryLocationRepository.findById(prevLocationId.longValue())
                    .orElseThrow(() -> new NotFoundException("Previous location not found"));
        } else {
            location = DeliveryLocation.builder()
                    .user(user)
                    .latitude(latitude)
                    .longitude(longitude)
                    .locationName(locationName)
                    .nickname(nickname)
                    .build();

            deliveryLocationRepository.save(location);
        }

        if(!isSameLocationId(location.getId())) {
            isSameLocationId(location.getId());
            user.setDeliveryLocation(location);
            userRepository.save(user);

            return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Delivery location updated successfully")
                .build();
        }
        else {
            return Response.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .message("Current location is the same as the previous one. No need to update it. Please try again with a different location.")
                    .build();
        }
    }

    public boolean isSameLocationId(Long locationId) {
        User user = userService.getCurrentLoggedInUser();
        DeliveryLocation activeLocation = user.getDeliveryLocation();

        if (activeLocation == null) return false; // no active location yet
        return activeLocation.getId().equals(locationId);
    }



    @Override
    public Response<DeliveryLocationDTO> deliveryActiveLocation() {
        User user = userService.getCurrentLoggedInUser();

        DeliveryLocation activeLocation = user.getDeliveryLocation();

        DeliveryLocationDTO dto = null;

        if (activeLocation != null) {
            dto = modelMapper.map(activeLocation, DeliveryLocationDTO.class);
            dto.setUserId(user.getId());
        }

        return Response.<DeliveryLocationDTO>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Success")
                .data(dto)
                .build();
    }
    @Override
    public Response<List<DeliveryLocationDTO>> allDeliveryForUser() {
        User user = userService.getCurrentLoggedInUser();

        Long defaultLocationId = (user.getDeliveryLocation() != null)
                ? user.getDeliveryLocation().getId()
                : null;

        List<DeliveryLocation> locations = deliveryLocationRepository.findAllByUser(user);

        List<DeliveryLocationDTO> dtos = locations.stream()
                .map(location -> {
                    DeliveryLocationDTO dto = modelMapper.map(location, DeliveryLocationDTO.class);
                    dto.setUserId(user.getId());
                    boolean isDefault = location.getId().equals(defaultLocationId);
                    dto.setIsDefault(isDefault);
                    return dto;
                })
                .collect(Collectors.toList());
        return Response.<List<DeliveryLocationDTO>>builder()
                .statusCode(HttpStatus.OK.value())
                .message("Success")
                .data(dtos)
                .build();
    }

    @Override
    @Transactional
    public Response<?> deleteDeliveryLocation(Long id) {
        Optional<DeliveryLocation> locationOptional = deliveryLocationRepository.findById(id);
        if (locationOptional.isEmpty()) {
            return Response.builder()
                    .statusCode(HttpStatus.NOT_FOUND.value())
                    .message("Delivery location with ID " + id + " not found.")
                    .build();
        }

        DeliveryLocation locationToDelete = locationOptional.get();
        User currentUser = userService.getCurrentLoggedInUser();

        if (!locationToDelete.getUser().getId().equals(currentUser.getId())) {
            return Response.builder()
                    .statusCode(HttpStatus.FORBIDDEN.value())
                    .message("You are not authorized to delete this location.")
                    .build();
        }

        if (currentUser.getDeliveryLocation() != null &&
                currentUser.getDeliveryLocation().getId().equals(locationToDelete.getId())) {

            currentUser.setDeliveryLocation(null);
            userRepository.save(currentUser);
        }

        deliveryLocationRepository.delete(locationToDelete);

        return Response.builder()
                .statusCode(HttpStatus.OK.value())
                .message("Location deleted successfully.")
                .build();
    }


}
