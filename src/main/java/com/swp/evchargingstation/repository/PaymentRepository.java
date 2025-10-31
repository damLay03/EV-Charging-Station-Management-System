package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Payment;
import com.swp.evchargingstation.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    // Thêm method tìm payment theo charging session
    Optional<Payment> findByChargingSession(ChargingSession chargingSession);

    Optional<Payment> findByTxnReference(String txnReference);

    // Tìm payment theo transaction ID (cho ZaloPay callback)
    Optional<Payment> findByTransactionId(String transactionId);

    // Query lấy doanh thu theo tuần của từng trạm (Thứ 2 - Chủ Nhật)
    // Sử dụng native query vì HQL không hỗ trợ WEEK() function với mode parameter
    @Query(value = "SELECT s.station_id, s.name, s.address, " +
            "WEEK(p.paid_at, 1) AS weekOfYear, " +
            "YEAR(p.paid_at), " +
            "SUM(p.amount), COUNT(p.payment_id) " +
            "FROM payments p " +
            "JOIN charging_sessions cs ON p.session_id = cs.session_id " +
            "JOIN charging_points cp ON cs.point_id = cp.point_id " +
            "JOIN stations s ON cp.station_id = s.station_id " +
            "WHERE WEEK(p.paid_at, 1) = :week " +
            "AND YEAR(p.paid_at) = :year " +
            "AND p.status = 'COMPLETED' " +
            "GROUP BY s.station_id, s.name, s.address, " +
            "WEEK(p.paid_at, 1), YEAR(p.paid_at) " +
            "ORDER BY s.name", nativeQuery = true)
    List<Object[]> findWeeklyRevenueByStation(@Param("year") int year, @Param("week") int week);

    // Query lấy doanh thu theo tháng của từng trạm
    @Query("SELECT s.stationId, s.name, s.address, " +
            "MONTH(p.paidAt), YEAR(p.paidAt), " +
            "SUM(p.amount), COUNT(p.paymentId) " +
            "FROM Payment p " +
            "JOIN p.chargingSession cs " +
            "JOIN cs.chargingPoint cp " +
            "JOIN cp.station s " +
            "WHERE YEAR(p.paidAt) = :year " +
            "AND MONTH(p.paidAt) = :month " +
            "AND p.status = 'COMPLETED' " +
            "GROUP BY s.stationId, s.name, s.address, " +
            "MONTH(p.paidAt), YEAR(p.paidAt)")
    List<Object[]> findMonthlyRevenueByStation(@Param("year") int year, @Param("month") int month);

    // Query lấy doanh thu theo năm của từng trạm (tất cả các tháng)
    @Query("SELECT s.stationId, s.name, s.address, " +
            "MONTH(p.paidAt), YEAR(p.paidAt), " +
            "SUM(p.amount), COUNT(p.paymentId) " +
            "FROM Payment p " +
            "JOIN p.chargingSession cs " +
            "JOIN cs.chargingPoint cp " +
            "JOIN cp.station s " +
            "WHERE YEAR(p.paidAt) = :year " +
            "AND p.status = 'COMPLETED' " +
            "GROUP BY s.stationId, s.name, s.address, " +
            "MONTH(p.paidAt), YEAR(p.paidAt) " +
            "ORDER BY MONTH(p.paidAt), s.name")
    List<Object[]> findYearlyRevenueByStation(@Param("year") int year);

    // Query tính tổng doanh thu trong tháng hiện tại (cho overview)
    @Query("SELECT SUM(p.amount) " +
            "FROM Payment p " +
            "WHERE YEAR(p.paidAt) = :year " +
            "AND MONTH(p.paidAt) = :month " +
            "AND p.status = 'COMPLETED'")
    Float findCurrentMonthRevenue(@Param("year") int year, @Param("month") int month);

    // Tìm payments theo status và station
    @Query("SELECT p FROM Payment p " +
            "JOIN p.chargingSession cs " +
            "JOIN cs.chargingPoint cp " +
            "WHERE cp.station.stationId = :stationId " +
            "AND p.status = :status " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findByStationIdAndStatus(@Param("stationId") String stationId, @Param("status") PaymentStatus status);

    // Tìm pending cash payment requests theo station
    @Query("SELECT p FROM Payment p " +
            "JOIN p.chargingSession cs " +
            "JOIN cs.chargingPoint cp " +
            "WHERE cp.station.stationId = :stationId " +
            "AND p.status = 'PENDING' " +
            "AND p.paymentMethod = 'CASH' " +
            "AND p.assignedStaff IS NOT NULL " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findPendingCashPaymentsByStationId(@Param("stationId") String stationId);

    // Tìm lịch sử cash payments đã được xác nhận bởi staff
    @Query("SELECT p FROM Payment p " +
            "WHERE p.confirmedByStaff.userId = :staffId " +
            "AND p.paymentMethod = 'CASH' " +
            "AND p.status = 'COMPLETED' " +
            "ORDER BY p.confirmedAt DESC")
    List<Payment> findConfirmedCashPaymentsByStaffId(@Param("staffId") String staffId);

    // Check if payment exists for a session
    boolean existsByChargingSession_SessionId(String sessionId);

    boolean existsByChargingSessionAndStatus(ChargingSession chargingSession, PaymentStatus status);
}