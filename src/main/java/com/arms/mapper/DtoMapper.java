package com.arms.mapper;

import com.arms.dto.ApartmentDtos;
import com.arms.dto.PaymentDtos;
import com.arms.dto.RentalDtos;
import com.arms.dto.RoomDtos;
import com.arms.dto.TenantDtos;
import com.arms.dto.UserDtos;
import com.arms.entity.Apartment;
import com.arms.entity.AppUser;
import com.arms.entity.Payment;
import com.arms.entity.Rental;
import com.arms.entity.Room;
import com.arms.entity.Tenant;

import java.math.BigDecimal;

/** Maps entities to their API representations. All mapping runs inside a transaction. */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static ApartmentDtos.ApartmentResponse toResponse(Apartment apartment) {
        return new ApartmentDtos.ApartmentResponse(
                apartment.getId(),
                apartment.getName(),
                apartment.getLocation(),
                apartment.getAddress(),
                apartment.getDescription(),
                apartment.isActive());
    }

    public static RoomDtos.RoomResponse toResponse(Room room) {
        return new RoomDtos.RoomResponse(
                room.getId(),
                room.getApartment().getId(),
                room.getApartment().getName(),
                room.getRoomNumber(),
                room.getRoomType(),
                room.getStatus().name(),
                room.getMaintenanceReason(),
                room.getRentAmount(),
                room.isActive());
    }

    public static TenantDtos.TenantResponse toResponse(Tenant tenant) {
        return new TenantDtos.TenantResponse(
                tenant.getId(),
                tenant.getFullName(),
                tenant.getPhone(),
                tenant.getEmail(),
                tenant.getAddress());
    }

    public static PaymentDtos.PaymentResponse toResponse(Payment payment) {
        return new PaymentDtos.PaymentResponse(
                payment.getId(),
                payment.getRental().getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getReversesPaymentId(),
                payment.getReversalReason(),
                payment.getCreatedAt());
    }

    public static UserDtos.UserResponse toResponse(AppUser user) {
        return new UserDtos.UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                user.isEnabled(),
                user.isMustChangePassword(),
                user.getCreatedAt());
    }

    /** @param totalPaid sum of PAID payments already computed by the caller. */
    public static RentalDtos.RentalResponse toResponse(Rental rental, BigDecimal totalPaid) {
        BigDecimal paid = totalPaid == null ? BigDecimal.ZERO : totalPaid;
        BigDecimal rentDue = rental.getRentAmount();
        AppUser creator = rental.getCreatedBy();
        return new RentalDtos.RentalResponse(
                rental.getId(),
                rental.getTenant().getId(),
                rental.getTenant().getFullName(),
                rental.getRoom().getId(),
                rental.getRoom().getApartment().getId(),
                rental.getRoom().getApartment().getName(),
                rental.getRoom().getRoomNumber(),
                rental.getStartDate(),
                rental.getEndDate(),
                rental.getStatus().name(),
                rentDue,
                paid,
                rentDue.subtract(paid),
                creator != null ? creator.getId() : null,
                creator != null ? creator.getUsername() : null,
                rental.getCreatedAt());
    }
}
