package com.helvino.ecommerce.integration.mpesa;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MpesaService {

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    @Value("${mpesa.callback-url}")
    private String callbackUrl;

    @Value("${mpesa.environment}")
    private String environment;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getBaseUrl() {
        return environment.equals("production")
                ? "https://api.safaricom.co.ke"
                : "https://sandbox.safaricom.co.ke";
    }

    public String getAccessToken() throws IOException {
        String credentials = consumerKey + ":" + consumerSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/oauth/v1/generate?grant_type=client_credentials")
                .get()
                .addHeader("Authorization", "Basic " + encoded)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body().string();
            Map<?, ?> data = objectMapper.readValue(body, Map.class);
            return (String) data.get("access_token");
        }
    }

    public MpesaStkResponse initiateSTKPush(String phone, double amount, String reference, String description) {
        try {
            String token = getAccessToken();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = Base64.getEncoder().encodeToString(
                    (shortcode + passkey + timestamp).getBytes());

            String sanitizedPhone = phone.startsWith("0") ? "254" + phone.substring(1) : phone;

            Map<String, Object> payload = new HashMap<>();
            payload.put("BusinessShortCode", shortcode);
            payload.put("Password", password);
            payload.put("Timestamp", timestamp);
            payload.put("TransactionType", "CustomerPayBillOnline");
            payload.put("Amount", (int) Math.ceil(amount));
            payload.put("PartyA", sanitizedPhone);
            payload.put("PartyB", shortcode);
            payload.put("PhoneNumber", sanitizedPhone);
            payload.put("CallBackURL", callbackUrl);
            payload.put("AccountReference", reference);
            payload.put("TransactionDesc", description);

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(payload),
                    MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(getBaseUrl() + "/mpesa/stkpush/v1/processrequest")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body().string();
                log.info("M-Pesa STK Push response: {}", responseBody);
                Map<?, ?> data = objectMapper.readValue(responseBody, Map.class);

                return MpesaStkResponse.builder()
                        .merchantRequestId((String) data.get("MerchantRequestID"))
                        .checkoutRequestId((String) data.get("CheckoutRequestID"))
                        .responseCode((String) data.get("ResponseCode"))
                        .responseDescription((String) data.get("ResponseDescription"))
                        .customerMessage((String) data.get("CustomerMessage"))
                        .build();
            }
        } catch (Exception e) {
            log.error("M-Pesa STK Push failed", e);
            throw new RuntimeException("M-Pesa payment initiation failed: " + e.getMessage());
        }
    }

    public Map<String, Object> querySTKStatus(String checkoutRequestId) throws IOException {
        String token = getAccessToken();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String password = Base64.getEncoder().encodeToString(
                (shortcode + passkey + timestamp).getBytes());

        Map<String, Object> payload = new HashMap<>();
        payload.put("BusinessShortCode", shortcode);
        payload.put("Password", password);
        payload.put("Timestamp", timestamp);
        payload.put("CheckoutRequestID", checkoutRequestId);

        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsString(payload),
                MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/mpesa/stkpushquery/v1/query")
                .post(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            return objectMapper.readValue(response.body().string(), Map.class);
        }
    }
}
