package com.toni.FoodApp.payment.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PokSdkOrder {
    private String id;
    @JsonProperty("_self")
    private SelfLink self = new SelfLink();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SelfLink {
        private String confirmUrl;
    }

    private Instant createdAt;
    private Instant expiresAt;

    private Boolean isRefunded;
    private Boolean isCanceled;
}