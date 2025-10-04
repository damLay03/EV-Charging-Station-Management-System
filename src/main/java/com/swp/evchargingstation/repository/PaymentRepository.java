package com.swp.evchargingstation.repository;

import com.swp.evchargingstation.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    // Query lấy doanh thu theo tháng của từng trạm
    @Query("SELECT s.stationId, s.name, s.address, " +
            "MONTH(p.paymentTime), YEAR(p.paymentTime), " +
            "SUM(p.amount), COUNT(p.paymentId) " +
            "FROM Payment p " +
            "JOIN p.session cs " +
            "JOIN cs.chargingPoint cp " +
            "JOIN cp.station s " +
            "WHERE YEAR(p.paymentTime) = :year " +
            "AND MONTH(p.paymentTime) = :month " +
            "AND p.status = 'COMPLETED' " +
            "GROUP BY s.stationId, s.name, s.address, " +
            "MONTH(p.paymentTime), YEAR(p.paymentTime)")
    List<Object[]> findMonthlyRevenueByStation(@Param("year") int year, @Param("month") int month);

    // Query lấy doanh thu theo năm của từng trạm (tất cả các tháng)
    @Query("SELECT s.stationId, s.name, s.address, " +
            "MONTH(p.paymentTime), YEAR(p.paymentTime), " +
            "SUM(p.amount), COUNT(p.paymentId) " +
            "FROM Payment p " +
            "JOIN p.session cs " +
            "JOIN cs.chargingPoint cp " +
            "JOIN cp.station s " +
            "WHERE YEAR(p.paymentTime) = :year " +
            "AND p.status = 'COMPLETED' " +
            "GROUP BY s.stationId, s.name, s.address, " +
            "MONTH(p.paymentTime), YEAR(p.paymentTime) " +
            "ORDER BY MONTH(p.paymentTime), s.name")
    List<Object[]> findYearlyRevenueByStation(@Param("year") int year);

    // Query tính tổng doanh thu trong tháng hiện tại (cho overview)
    @Query("SELECT SUM(p.amount) " +
            "FROM Payment p " +
            "WHERE YEAR(p.paymentTime) = :year " +
            "AND MONTH(p.paymentTime) = :month " +
            "AND p.status = 'COMPLETED'")
    Float findCurrentMonthRevenue(@Param("year") int year, @Param("month") int month);
}