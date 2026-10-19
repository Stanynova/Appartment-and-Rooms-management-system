package com.arms.repository;

import com.arms.entity.Room;
import com.arms.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /** FR-1.2: a room number is unique within its apartment. */
    boolean existsByApartmentIdAndRoomNumberIgnoreCase(Long apartmentId, String roomNumber);

    /** FR-1.2: uniqueness re-checked on update, ignoring the room being edited. */
    boolean existsByApartmentIdAndRoomNumberIgnoreCaseAndIdNot(Long apartmentId, String roomNumber, Long id);

    List<Room> findByApartmentIdOrderByRoomNumberAsc(Long apartmentId);

    List<Room> findByApartmentIdAndStatusOrderByRoomNumberAsc(Long apartmentId, RoomStatus status);

    List<Room> findByActiveTrueOrderByRoomNumberAsc();

    List<Room> findByStatusOrderByRoomNumberAsc(RoomStatus status);

    /** FR-6.5 dashboard and FR-6.1 occupancy figures. */
    long countByActiveTrue();

    long countByActiveTrueAndStatus(RoomStatus status);

    /** FR-5.3: search rooms by room number. */
    @Query("""
            select r from Room r
            where lower(r.roomNumber) like lower(concat('%', :q, '%'))
            order by r.roomNumber
            """)
    List<Room> searchByNumber(@Param("q") String query);
}
