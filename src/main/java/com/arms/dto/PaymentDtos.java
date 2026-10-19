package com.arms.dto;

import com.arms.entity.PaymentMethod;
import com.arms.entity.PaymentStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Payment endpoints (SRS FR-4.1 to FR-4.6). */
public final class PaymentDtos {

    private PaymentDtos() {
    }

    public record CreatePaymentRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
            /** Defaults to today when omitted. */
            LocalDate paymentDate,
            @NotNull PaymentMethod method,
            /** Defaults to PAID when omitted (FR-4.4). */
            PaymentStatus status) {
    }

    /** FR-4.6: correcting a payment recorded in error by a reversing entry. */
    public record ReversePaymentRequest(
            @NotBlank @Size(max = 255) String reason) {
    }

    public record PaymentResponse(
            Long id,
            Long rentalId,
            BigDecimal amount,
            LocalDate paymentDate,
            String method,
            String status,
            Long reversesPaymentId,
            String reversalReason,
            LocalDateTime createdAt) {
    }

    public record PaymentSummaryResponse(
            Long rentalId,
            BigDecimal rentDue,
            BigDecimal totalPaid,
            BigDecimal balanceOutstanding,
            List<PaymentResponse> payments) {
    }
}
