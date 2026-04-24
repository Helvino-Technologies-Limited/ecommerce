package com.helvino.ecommerce.integration.mpesa;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MpesaStkResponse {
    private String merchantRequestId;
    private String checkoutRequestId;
    private String responseCode;
    private String responseDescription;
    private String customerMessage;

    public boolean isSuccess() {
        return "0".equals(responseCode);
    }
}
