package com.toni.FoodApp.payment.webhook.httpRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toni.FoodApp.exceptions.PaymentGatewayException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
@Component
@RequiredArgsConstructor
public class PostRequest {
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    /**
     * Executes a generic POST request and parses the JSON response.
     *
     * @param url          The target URL.
     * @param requestBody  The payload to send (Type T).
     * @param token        The Bearer token for authorization.
     * @param responseType The class type to deserialize the response into (Type R).
     * @param apiName      A descriptive name for error logging (e.g., "Pok order").
     * @return The parsed response object (Type R).
     */
    public <T, R> R executePostRequest(String url, T requestBody, String token, Class<R> responseType, String apiName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<T> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
                try {
                    return objectMapper.readValue(response.getBody(), responseType);
                } catch (JsonProcessingException e) {
                    throw new PaymentGatewayException("Failed to parse successful " + apiName + " response: " + e.getMessage(), e);
                }
            }

            throw new PaymentGatewayException(
                    apiName + " API returned an unexpected status. Status: " + response.getStatusCode() + ", Body: " + response.getBody()
            );

        } catch (RestClientException e) {
            throw new PaymentGatewayException("Error creating " + apiName + ": " + e.getMessage(), e);
        }
    }
}
