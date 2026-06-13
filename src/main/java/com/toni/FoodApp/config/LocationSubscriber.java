package com.toni.FoodApp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toni.FoodApp.deliverTo.dtos.LocationPayload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class LocationSubscriber {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public LocationSubscriber(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void handleLocationUpdate(String message) {

        try {

            LocationPayload location =
                    objectMapper.readValue(
                            message,
                            LocationPayload.class
                    );

            messagingTemplate.convertAndSend(
                    location
            );

        } catch (Exception e) {

            System.err.println(
                    "Failed to parse location update: "
                            + e.getMessage()
            );
        }
    }
}