package com.toni.FoodApp.payment.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

// This is the new top-level wrapper for the entire response
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PokApiOrderResponse {
    private DataPayload data;
    private String message;
    private int statusCode;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataPayload {
        private PokSdkOrder sdkOrder;
    }
}