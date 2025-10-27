-- Migration: Thêm các cột vào bảng payments để hỗ trợ thanh toán tiền mặt
-- Không cần tạo bảng mới, chỉ cần thêm 3 cột vào bảng payments có sẵn

ALTER TABLE payments
ADD COLUMN assigned_staff_id VARCHAR(36) NULL COMMENT 'Staff được assign xử lý thanh toán tiền mặt',
ADD COLUMN confirmed_by_staff_id VARCHAR(36) NULL COMMENT 'Staff đã xác nhận thanh toán tiền mặt',
ADD COLUMN confirmed_at TIMESTAMP NULL COMMENT 'Thời gian staff xác nhận thanh toán tiền mặt';

-- Thêm foreign key constraints
ALTER TABLE payments
ADD CONSTRAINT fk_payment_assigned_staff
    FOREIGN KEY (assigned_staff_id) REFERENCES staffs(user_id) ON DELETE SET NULL;

ALTER TABLE payments
ADD CONSTRAINT fk_payment_confirmed_by_staff
    FOREIGN KEY (confirmed_by_staff_id) REFERENCES staffs(user_id) ON DELETE SET NULL;

-- Tạo index để tối ưu query
CREATE INDEX idx_payment_assigned_staff
    ON payments(assigned_staff_id);

CREATE INDEX idx_payment_confirmed_by_staff
    ON payments(confirmed_by_staff_id);

-- Ghi chú:
-- Field payment_method đã có sẵn để phân biệt CASH vs VNPAY
-- Field status đã có PENDING_CASH để đánh dấu đang chờ staff xác nhận
