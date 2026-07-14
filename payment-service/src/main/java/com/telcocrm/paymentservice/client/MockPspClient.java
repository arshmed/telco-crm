package com.telcocrm.paymentservice.client;

import com.telcocrm.paymentservice.client.dto.PspChargeResult;
import com.telcocrm.paymentservice.entity.enums.PaymentMethod;

import java.math.BigDecimal;

public interface MockPspClient {

    PspChargeResult charge(BigDecimal amount, PaymentMethod method);
}
