package com.arms.service;

import com.arms.dto.PaymentDtos.CreatePaymentRequest;
import com.arms.dto.PaymentDtos.PaymentResponse;
import com.arms.dto.PaymentDtos.PaymentSummaryResponse;
import com.arms.dto.PaymentDtos.ReversePaymentRequest;
import com.arms.entity.Payment;
import com.arms.entity.PaymentStatus;
import com.arms.entity.Rental;
import com.arms.entity.RentalStatus;
import com.arms.exception.ConflictException;
import com.arms.exception.NotFoundException;
import com.arms.mapper.DtoMapper;
import com.arms.repository.PaymentRepository;
import com.arms.repository.RentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Payment management (SRS FR-4.1 to FR-4.6).
 *
 * <p>Payments are never deleted: an error is corrected with a reversing entry
 * that records the reason (FR-4.6). The outstanding balance is the rent due
 * less the payments recorded (FR-4.3, BR-5).</p>
 */
@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final RentalRepository rentals;
    private final Messages messages;

    public PaymentService(PaymentRepository payments, RentalRepository rentals, Messages messages) {
        this.payments = payments;
        this.rentals = rentals;
        this.messages = messages;
    }

    /** FR-4.1 / FR-4.5: any number of payments may be recorded per rental. */
    @Transactional
    public PaymentResponse add(Long rentalId, CreatePaymentRequest request) {
        Rental rental = requireRental(rentalId);
        if (rental.getStatus() == RentalStatus.CANCELLED) {
            throw new ConflictException(messages.get("payment.rentalCancelled"), "RENTAL_CANCELLED");
        }

        Payment payment = new Payment();
        payment.setRental(rental);
        payment.setAmount(request.amount());
        payment.setPaymentDate(request.paymentDate() != null
                ? request.paymentDate()
                : LocalDate.now());
        payment.setMethod(request.method());
        payment.setStatus(request.status() != null ? request.status() : PaymentStatus.PAID);
        payments.save(payment);
        return DtoMapper.toResponse(payment);
    }

    /** FR-4.3: rent due, amounts paid to date and the balance outstanding. */
    @Transactional(readOnly = true)
    public PaymentSummaryResponse summary(Long rentalId) {
        Rental rental = requireRental(rentalId);
        List<PaymentResponse> entries = payments.findByRentalIdOrderByPaymentDateAsc(rentalId).stream()
                .map(DtoMapper::toResponse)
                .toList();
        BigDecimal rentDue = rental.getRentAmount();
        BigDecimal totalPaid = paidSum(rentalId);
        return new PaymentSummaryResponse(
                rentalId, rentDue, totalPaid, rentDue.subtract(totalPaid), entries);
    }

    @Transactional(readOnly = true)
    public PaymentResponse get(Long id) {
        return DtoMapper.toResponse(requirePayment(id));
    }

    /** FR-4.6: record a reversing entry with the reason; no payment is deleted. */
    @Transactional
    public PaymentResponse reverse(Long paymentId, ReversePaymentRequest request) {
        Payment original = requirePayment(paymentId);
        if (original.getReversesPaymentId() != null) {
            throw new ConflictException(
                    messages.get("payment.reversalTarget"), "REVERSING_ENTRY");
        }
        if (payments.existsByReversesPaymentId(paymentId)) {
            throw new ConflictException(
                    messages.get("payment.alreadyReversed"), "ALREADY_REVERSED");
        }

        Payment reversal = new Payment();
        reversal.setRental(original.getRental());
        reversal.setAmount(original.getAmount().negate());
        reversal.setPaymentDate(LocalDate.now());
        reversal.setMethod(original.getMethod());
        reversal.setStatus(original.getStatus());
        reversal.setReversesPaymentId(original.getId());
        reversal.setReversalReason(request.reason().trim());
        payments.save(reversal);
        return DtoMapper.toResponse(reversal);
    }

    private BigDecimal paidSum(Long rentalId) {
        BigDecimal sum = payments.sumByRentalIdAndStatus(rentalId, PaymentStatus.PAID);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private Rental requireRental(Long rentalId) {
        return rentals.findById(rentalId)
                .orElseThrow(() -> new NotFoundException(
                        messages.get("payment.rentalNotFound"), "RENTAL_NOT_FOUND"));
    }

    private Payment requirePayment(Long paymentId) {
        return payments.findById(paymentId)
                .orElseThrow(() -> new NotFoundException(
                        messages.get("payment.notFound"), "PAYMENT_NOT_FOUND"));
    }
}
