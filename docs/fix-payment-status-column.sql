-- Fix: Sửa logic thanh toán - xóa PENDING_CASH, chỉ giữ PENDING
-- Lý do: Driver chọn phương thức thanh toán CASH/VNPAY sau khi session hoàn thành
--        Status chỉ cần PENDING (đang chờ xử lý) thay vì PENDING_CASH

-- KHÔNG CẦN thay đổi database schema vì:
-- 1. Cột status đã có thể chứa giá trị PENDING
-- 2. Chỉ cần sửa logic code để:
--    - Dùng status = 'PENDING' thay vì 'PENDING_CASH'
--    - Phân biệt cash payment request bằng: paymentMethod = 'CASH' + assignedStaff IS NOT NULL

-- Nếu database có data PENDING_CASH cũ, chạy query này để update:
UPDATE payments
SET status = 'PENDING'
WHERE status = 'PENDING_CASH';

-- Ghi chú:
-- - Payment status = PENDING: đang chờ xử lý (chờ driver chọn phương thức hoặc chờ staff xác nhận)
-- - Payment status = COMPLETED: đã hoàn thành
-- - paymentMethod = 'CASH': thanh toán bằng tiền mặt
-- - paymentMethod = 'VNPAY': thanh toán qua VNPay
-- - assignedStaff != NULL: đã được assign cho staff xử lý (với cash payment)
