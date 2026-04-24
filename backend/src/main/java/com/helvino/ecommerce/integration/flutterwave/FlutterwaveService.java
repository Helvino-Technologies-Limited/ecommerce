package com.helvino.ecommerce.integration.flutterwave;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlutterwaveService {

    @Value("${flutterwave.secret-key:}")
    private String secretKey;

    @Value("${flutterwave.callback-url:https://shop.helvino.org/payment/verify}")
    private String callbackUrl;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "https://api.flutterwave.com/v3";

    public Map<String, Object> initiatePayment(FlutterwavePaymentRequest req) {
        try {
            Map<String, Object> customer = new HashMap<>();
            customer.put("email", req.getCustomerEmail());
            customer.put("name", req.getCustomerName());
            customer.put("phonenumber", req.getCustomerPhone());

            Map<String, Object> payload = new HashMap<>();
            payload.put("tx_ref", req.getTxRef() != null ? req.getTxRef() : UUID.randomUUID().toString());
            payload.put("amount", req.getAmount());
            payload.put("currency", req.getCurrency());
            payload.put("redirect_url", callbackUrl);
            payload.put("customer", customer);
            payload.put("customizations", Map.of(
                    "title", "Helvino Shop",
                    "description", req.getDescription(),
                    "logo", "https://helvino.org/logo.png"));

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(BASE_URL + "/payments")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + secretKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                log.info("Flutterwave payment init: {}", responseBody);
                return objectMapper.readValue(responseBody, Map.class);
            }
        } catch (Exception e) {
            log.error("Flutterwave payment initiation failed", e);
            throw new RuntimeException("Card payment initiation failed: " + e.getMessage());
        }
    }

    public Map<String, Object> verifyTransaction(String transactionId) {
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/transactions/" + transactionId + "/verify")
                    .get()
                    .addHeader("Authorization", "Bearer " + secretKey)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return objectMapper.readValue(response.body().string(), Map.class);
            }
        } catch (Exception e) {
            log.error("Flutterwave verification failed", e);
            throw new RuntimeException("Transaction verification failed: " + e.getMessage());
        }
    }
}
