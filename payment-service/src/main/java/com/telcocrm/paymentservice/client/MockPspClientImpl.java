package com.telcocrm.paymentservice.client;

import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class MockPspClientImpl implements MockPspClient {

    private static final double SUCCESS_RATE = 0.85;
    private static final String[] FAILURE_REASONS = {"Insufficient funds", "Card declined", "Card expired"};

    @Override
    public PspChargeResult charge(BigDecimal amount, PaymentMethod method) {
        simulateNetworkLatency();

        if (Math.random() < SUCCESS_RATE) {
            return new PspChargeResult(true, "MOCK-REF-" + UUID.randomUUID(), null);
        }

        String failureReason = FAILURE_REASONS[(int) (Math.random() * FAILURE_REASONS.length)];
        return new PspChargeResult(false, null, failureReason);
    }

    private void simulateNetworkLatency() {
        try {
            Thread.sleep(100 + (long) (Math.random() * 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
