package com.helvino.ecommerce.controller;

import com.helvino.ecommerce.entity.Order;
import com.helvino.ecommerce.enums.PaymentMethod;
import com.helvino.ecommerce.enums.PaymentStatus;
import com.helvino.ecommerce.integration.flutterwave.FlutterwavePaymentRequest;
import com.helvino.ecommerce.integration.flutterwave.FlutterwaveService;
import com.helvino.ecommerce.integration.mpesa.MpesaService;
import com.helvino.ecommerce.integration.mpesa.MpesaStkResponse;
import com.helvino.ecommerce.repository.OrderRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final MpesaService mpesaService;
    private final FlutterwaveService flutterwaveService;
    private final OrderRepository orderRepository;

    @PostMapping("/mpesa/stk-push")
    public ResponseEntity<?> mpesaSTKPush(@RequestBody MpesaPayRequest req) {
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        MpesaStkResponse response = mpesaService.initiateSTKPush(
                req.getPhone(),
                order.getTotalAmount().doubleValue(),
                order.getOrderNumber(),
                "Payment for order " + order.getOrderNumber());

        if (response.isSuccess()) {
            order.setPaymentMethod(PaymentMethod.MPESA);
            order.setPaymentReference(response.getCheckoutRequestId());
            orderRepository.save(order);
        }

        return ResponseEntity.ok(Map.of(
                "checkoutRequestId", response.getCheckoutRequestId(),
                "message", response.getCustomerMessage()));
    }

    @PostMapping("/mpesa/callback")
    public ResponseEntity<String> mpesaCallback(@RequestBody Map<String, Object> payload) {
        log.info("M-Pesa callback received: {}", payload);
        try {
            Map<?, ?> body = (Map<?, ?>) payload.get("Body");
            Map<?, ?> stkCallback = (Map<?, ?>) body.get("stkCallback");
            String resultCode = String.valueOf(stkCallback.get("ResultCode"));
            String checkoutRequestId = (String) stkCallback.get("CheckoutRequestID");

            Order order = orderRepository.findAll().stream()
                    .filter(o -> checkoutRequestId.equals(o.getPaymentReference()))
                    .findFirst().orElse(null);

            if (order != null) {
                if ("0".equals(resultCode)) {
                    Map<?, ?> callbackMetadata = (Map<?, ?>) stkCallback.get("CallbackMetadata");
                    if (callbackMetadata != null) {
                        var items = (java.util.List<?>) callbackMetadata.get("Item");
                        for (Object item : items) {
                            Map<?, ?> i = (Map<?, ?>) item;
                            if ("MpesaReceiptNumber".equals(i.get("Name"))) {
                                order.setMpesaReceiptNumber((String) i.get("Value"));
                            }
                        }
                    }
                    order.setPaymentStatus(PaymentStatus.COMPLETED);
                } else {
                    order.setPaymentStatus(PaymentStatus.FAILED);
                }
                orderRepository.save(order);
            }
        } catch (Exception e) {
            log.error("M-Pesa callback processing error", e);
        }
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/card/initiate")
    public ResponseEntity<?> initiateCardPayment(@RequestBody CardPayRequest req) {
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        var flwReq = FlutterwavePaymentRequest.builder()
                .amount(order.getTotalAmount().doubleValue())
                .currency(order.getCurrency().name())
                .customerEmail(order.getCustomer().getEmail())
                .customerName(order.getCustomer().getFullName())
                .customerPhone(order.getCustomer().getPhone())
                .description("Payment for order " + order.getOrderNumber())
                .txRef(order.getOrderNumber())
                .build();

        Map<String, Object> response = flutterwaveService.initiatePayment(flwReq);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/flutterwave/webhook")
    public ResponseEntity<String> flutterwaveWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Flutterwave webhook: {}", payload);
        try {
            String event = (String) payload.get("event");
            if ("charge.completed".equals(event)) {
                Map<?, ?> data = (Map<?, ?>) payload.get("data");
                String txRef = (String) data.get("tx_ref");
                String status = (String) data.get("status");

                orderRepository.findByOrderNumber(txRef).ifPresent(order -> {
                    if ("successful".equals(status)) {
                        order.setPaymentStatus(PaymentStatus.COMPLETED);
                        order.setPaymentMethod(PaymentMethod.CARD);
                        order.setPaymentReference(String.valueOf(data.get("id")));
                    } else {
                        order.setPaymentStatus(PaymentStatus.FAILED);
                    }
                    orderRepository.save(order);
                });
            }
        } catch (Exception e) {
            log.error("Flutterwave webhook error", e);
        }
        return ResponseEntity.ok("OK");
    }

    @Data
    public static class MpesaPayRequest {
        private UUID orderId;
        private String phone;
    }

    @Data
    public static class CardPayRequest {
        private UUID orderId;
    }
}
