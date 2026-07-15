package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.dto.request.CreatePaymentRequest;
import com.telcocrm.paymentservice.dto.request.RefundRequest;
import com.telcocrm.paymentservice.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {

    PaymentResponse getPaymentById(UUID paymentId);

    PaymentResponse refundPayment(UUID paymentId, RefundRequest request);

    PaymentResponse createPayment(CreatePaymentRequest request);
}
