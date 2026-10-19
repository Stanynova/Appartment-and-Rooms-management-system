package com.arms.controller;

import com.arms.dto.PaymentDtos.CreatePaymentRequest;
import com.arms.dto.PaymentDtos.PaymentResponse;
import com.arms.dto.PaymentDtos.PaymentSummaryResponse;
import com.arms.dto.PaymentDtos.ReversePaymentRequest;
import com.arms.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment management endpoints (SRS FR-4.1 to FR-4.6).
 *
 * <p>There is deliberately no DELETE endpoint: a payment recorded in error is
 * corrected by a reversing entry that records the reason (FR-4.6).</p>
 */
@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** FR-4.1 / FR-4.5: record a payment against a rental. */
    @PostMapping("/rentals/{rentalId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse add(@PathVariable Long rentalId,
                               @Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.add(rentalId, request);
    }

    /** FR-4.3: rent due, paid to date and the outstanding balance. */
    @GetMapping("/rentals/{rentalId}/payments")
    public PaymentSummaryResponse summary(@PathVariable Long rentalId) {
        return paymentService.summary(rentalId);
    }

    @GetMapping("/payments/{id}")
    public PaymentResponse get(@PathVariable Long id) {
        return paymentService.get(id);
    }

    /** FR-4.6: correct an error with a reversing entry, never by deletion. */
    @PostMapping("/payments/{id}/reverse")
    public PaymentResponse reverse(@PathVariable Long id,
                                   @Valid @RequestBody ReversePaymentRequest request) {
        return paymentService.reverse(id, request);
    }
}
