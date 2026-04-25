package com.helvino.ecommerce.service;

import com.helvino.ecommerce.entity.Tenant;
import com.helvino.ecommerce.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class TenantSubscriptionService {

    public static final String PAYBILL = "522533";
    public static final String ACCOUNT = "8071524";
    private static final int GRACE_DAYS = 5;

    public boolean hasSellerAccess(Tenant tenant) {
        return getInfo(tenant).isHasAccess();
    }

    public SubscriptionInfo getInfo(Tenant tenant) {
        if (tenant == null) return locked(null);

        LocalDate today = LocalDate.now();
        SubscriptionStatus status = tenant.getSubscriptionStatus();

        if (status == SubscriptionStatus.SUSPENDED || status == SubscriptionStatus.INACTIVE) {
            return SubscriptionInfo.builder()
                    .status(status.name())
                    .hasAccess(false)
                    .locked(true)
                    .inGracePeriod(false)
                    .paybillNumber(PAYBILL)
                    .accountNumber(ACCOUNT)
                    .monthlyFee(tenant.getMonthlyFee())
                    .businessName(tenant.getBusinessName())
                    .lastPaymentAt(tenant.getLastPaymentAt())
                    .totalPaymentsMade(tenant.getTotalPaymentsMade())
                    .build();
        }

        if (status == SubscriptionStatus.TRIAL) {
            LocalDate trialEndsAt = tenant.getTrialEndsAt();
            LocalDate gracePeriodEndsAt = trialEndsAt != null ? trialEndsAt.plusDays(GRACE_DAYS) : null;

            boolean pastGrace = gracePeriodEndsAt != null && today.isAfter(gracePeriodEndsAt);
            boolean inGrace = !pastGrace && trialEndsAt != null && today.isAfter(trialEndsAt);

            long daysUntilTrialEnds = trialEndsAt != null ? ChronoUnit.DAYS.between(today, trialEndsAt) : 0;
            long daysInGrace = inGrace && gracePeriodEndsAt != null
                    ? ChronoUnit.DAYS.between(today, gracePeriodEndsAt) : 0;

            return SubscriptionInfo.builder()
                    .status("TRIAL")
                    .trialEndsAt(trialEndsAt)
                    .gracePeriodEndsAt(gracePeriodEndsAt)
                    .daysUntilTrialEnds(Math.max(0, daysUntilTrialEnds))
                    .daysInGracePeriod(Math.max(0, daysInGrace))
                    .hasAccess(!pastGrace)
                    .locked(pastGrace)
                    .inGracePeriod(inGrace)
                    .paybillNumber(PAYBILL)
                    .accountNumber(ACCOUNT)
                    .monthlyFee(tenant.getMonthlyFee())
                    .businessName(tenant.getBusinessName())
                    .lastPaymentAt(tenant.getLastPaymentAt())
                    .totalPaymentsMade(tenant.getTotalPaymentsMade())
                    .build();
        }

        if (status == SubscriptionStatus.ACTIVE) {
            LocalDate renewsAt = tenant.getSubscriptionRenewsAt();
            LocalDate gracePeriodEndsAt = renewsAt != null ? renewsAt.plusDays(GRACE_DAYS) : null;

            boolean pastGrace = gracePeriodEndsAt != null && today.isAfter(gracePeriodEndsAt);
            boolean inGrace = !pastGrace && renewsAt != null && today.isAfter(renewsAt);

            long daysUntilRenewal = renewsAt != null ? ChronoUnit.DAYS.between(today, renewsAt) : Long.MAX_VALUE;
            long daysInGrace = inGrace && gracePeriodEndsAt != null
                    ? ChronoUnit.DAYS.between(today, gracePeriodEndsAt) : 0;

            return SubscriptionInfo.builder()
                    .status("ACTIVE")
                    .subscriptionRenewsAt(renewsAt)
                    .gracePeriodEndsAt(gracePeriodEndsAt)
                    .daysUntilRenewal(renewsAt != null ? Math.max(0, daysUntilRenewal) : null)
                    .daysInGracePeriod(Math.max(0, daysInGrace))
                    .hasAccess(!pastGrace)
                    .locked(pastGrace)
                    .inGracePeriod(inGrace)
                    .paybillNumber(PAYBILL)
                    .accountNumber(ACCOUNT)
                    .monthlyFee(tenant.getMonthlyFee())
                    .businessName(tenant.getBusinessName())
                    .lastPaymentAt(tenant.getLastPaymentAt())
                    .totalPaymentsMade(tenant.getTotalPaymentsMade())
                    .build();
        }

        return locked(tenant);
    }

    private SubscriptionInfo locked(Tenant tenant) {
        return SubscriptionInfo.builder()
                .status("INACTIVE")
                .hasAccess(false)
                .locked(true)
                .inGracePeriod(false)
                .paybillNumber(PAYBILL)
                .accountNumber(ACCOUNT)
                .monthlyFee(tenant != null ? tenant.getMonthlyFee() : new BigDecimal("500.00"))
                .businessName(tenant != null ? tenant.getBusinessName() : "")
                .lastPaymentAt(tenant != null ? tenant.getLastPaymentAt() : null)
                .totalPaymentsMade(tenant != null ? tenant.getTotalPaymentsMade() : 0)
                .build();
    }

    @Getter
    @Builder
    public static class SubscriptionInfo {
        private String status;
        private LocalDate trialEndsAt;
        private LocalDate subscriptionRenewsAt;
        private LocalDate gracePeriodEndsAt;
        private Long daysUntilTrialEnds;
        private Long daysUntilRenewal;
        private Long daysInGracePeriod;
        private boolean hasAccess;
        private boolean locked;
        private boolean inGracePeriod;
        private String paybillNumber;
        private String accountNumber;
        private BigDecimal monthlyFee;
        private String businessName;
        private LocalDate lastPaymentAt;
        private int totalPaymentsMade;
    }
}
