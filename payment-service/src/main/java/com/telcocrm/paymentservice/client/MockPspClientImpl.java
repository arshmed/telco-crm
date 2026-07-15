package com.telcocrm.paymentservice.client;

import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPspClientImpl implements MockPspClient {

    private static final String[] CREDIT_CARD_FAILURE_REASONS = {"Insufficient funds", "Card declined", "Card expired"};
    private static final String[] BANK_TRANSFER_FAILURE_REASONS = {"Bank account not found", "Transfer limit exceeded"};
    private static final String[] WALLET_FAILURE_REASONS = {"Wallet balance insufficient"};

    @Override
    public PspChargeResult charge(BigDecimal amount, PaymentMethod method) {
        PspProfile profile = profileFor(method);
        simulateNetworkLatency(profile.minDelayMs(), profile.maxDelayMs());

        if (Math.random() < profile.successRate()) {
            return new PspChargeResult(true, "MOCK-REF-" + UUID.randomUUID(), null);
        }

        String[] reasons = profile.failureReasons();
        String failureReason = reasons[(int) (Math.random() * reasons.length)];
        return new PspChargeResult(false, null, failureReason);
    }

    private PspProfile profileFor(PaymentMethod method) {
        return switch (method) {
            case CREDIT_CARD -> new PspProfile(0.85, 100, 300, CREDIT_CARD_FAILURE_REASONS);
            case BANK_TRANSFER -> new PspProfile(0.95, 500, 1000, BANK_TRANSFER_FAILURE_REASONS);
            case WALLET -> new PspProfile(0.98, 50, 150, WALLET_FAILURE_REASONS);
        };
    }

    private void simulateNetworkLatency(long minDelayMs, long maxDelayMs) {
        try {
            Thread.sleep(minDelayMs + (long) (Math.random() * (maxDelayMs - minDelayMs)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record PspProfile(double successRate, long minDelayMs, long maxDelayMs, String[] failureReasons) {}
}
