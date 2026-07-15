package com.telcocrm.paymentservice.client;

import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MockPspClientImplTest {

    private static final Set<String> CREDIT_CARD_REASONS = Set.of("Insufficient funds", "Card declined", "Card expired");
    private static final Set<String> BANK_TRANSFER_REASONS = Set.of("Bank account not found", "Transfer limit exceeded");
    private static final Set<String> WALLET_REASONS = Set.of("Wallet balance insufficient");

    private final MockPspClientImpl mockPspClient = new MockPspClientImpl();

    @ParameterizedTest
    @EnumSource(PaymentMethod.class)
    void charge_shouldReturnStructurallyValidResultForEveryMethod(PaymentMethod method) {
        // Happy path (statistical): over many attempts every outcome must be internally consistent.
        for (int i = 0; i < 50; i++) {
            PspChargeResult result = mockPspClient.charge(new BigDecimal("100.00"), method);

            assertThat(result).isNotNull();
            if (result.success()) {
                assertThat(result.externalRef()).startsWith("MOCK-REF-");
                assertThat(result.failureReason()).isNull();
            } else {
                assertThat(result.externalRef()).isNull();
                assertThat(result.failureReason()).isIn(reasonsFor(method));
            }
        }
    }

    @Test
    void charge_creditCardFailureReasons_shouldOnlyComeFromCreditCardPool() {
        boolean sawFailure = chargeUntilFailureSeen(PaymentMethod.CREDIT_CARD, CREDIT_CARD_REASONS);
        assertThat(sawFailure).as("at least one CREDIT_CARD failure should occur within 500 attempts").isTrue();
    }

    @Test
    void charge_bankTransferFailureReasons_shouldOnlyComeFromBankTransferPool() {
        boolean sawFailure = chargeUntilFailureSeen(PaymentMethod.BANK_TRANSFER, BANK_TRANSFER_REASONS);
        assertThat(sawFailure).as("at least one BANK_TRANSFER failure should occur within 500 attempts").isTrue();
    }

    @Test
    void charge_walletFailureReasons_shouldOnlyComeFromWalletPool() {
        boolean sawFailure = chargeUntilFailureSeen(PaymentMethod.WALLET, WALLET_REASONS);
        assertThat(sawFailure).as("at least one WALLET failure should occur within 500 attempts").isTrue();
    }

    @RepeatedTest(5)
    void charge_shouldGenerateUniqueExternalRefOnSuccess() {
        PspChargeResult first = mockPspClient.charge(new BigDecimal("10"), PaymentMethod.WALLET);
        PspChargeResult second = mockPspClient.charge(new BigDecimal("10"), PaymentMethod.WALLET);

        if (first.success() && second.success()) {
            assertThat(first.externalRef()).isNotEqualTo(second.externalRef());
        }
    }

    private boolean chargeUntilFailureSeen(PaymentMethod method, Set<String> expectedReasons) {
        for (int i = 0; i < 500; i++) {
            PspChargeResult result = mockPspClient.charge(BigDecimal.TEN, method);
            if (!result.success()) {
                assertThat(result.failureReason()).isIn(expectedReasons);
                return true;
            }
        }
        return false;
    }

    private Set<String> reasonsFor(PaymentMethod method) {
        return switch (method) {
            case CREDIT_CARD -> CREDIT_CARD_REASONS;
            case BANK_TRANSFER -> BANK_TRANSFER_REASONS;
            case WALLET -> WALLET_REASONS;
        };
    }
}
