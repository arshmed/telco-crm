package com.telcocrm.paymentservice.client.dto;

public record PspChargeResult(
    boolean success,
    String externalRef,
    String failureReason
) {}
