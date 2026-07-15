package com.telcocrm.paymentservice.service;

import com.telcocrm.paymentservice.dto.request.CreatePaymentRequest;
import com.telcocrm.paymentservice.dto.request.RefundRequest;
import com.telcocrm.paymentservice.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {

    Page<PaymentResponse> getAllPayments(Pageable pageable);

    PaymentResponse getPaymentById(UUID paymentId);

    PaymentResponse refundPayment(UUID paymentId, RefundRequest request);

    PaymentResponse createPayment(CreatePaymentRequest request);
}
