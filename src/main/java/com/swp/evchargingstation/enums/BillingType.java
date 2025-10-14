package com.swp.evchargingstation.enums;

/**
 * Plan fields: name(!blank, unique), billingType, pricePerKwh>=0, pricePerMinute>=0, monthlyFee>=0, benefits<=1000 chars.
 *
 * Công thức / Ràng buộc theo loại (theo PlanService.validateConfig):
 *  PAY_AS_YOU_GO:
 *    - monthlyFee == 0
 *    - (pricePerKwh >=0, pricePerMinute >=0)
 *    - Khuyến nghị: (pricePerKwh >0 OR pricePerMinute >0) để gói có giá.
 *  MONTHLY_SUBSCRIPTION:
 *    - monthlyFee > 0
 *    - pricePerKwh / pricePerMinute: >=0 (có thể 0 nếu ưu đãi)
 *  PREPAID:
 *    - monthlyFee == 0
 *    - (pricePerKwh > 0) OR (pricePerMinute > 0)
 *  POSTPAID:
 *    - monthlyFee == 0
 *    - (pricePerKwh > 0) OR (pricePerMinute > 0)
 *  VIP:
 *    - monthlyFee > 0
 *    - pricePerKwh >=0, pricePerMinute >=0 (có thể =0 => miễn phí / giảm mạnh)
 *
 * Ghi chú: Thay đổi rule phải chỉnh cả validateConfig() rồi update bảng trên.
 */
public enum BillingType {
    PAY_AS_YOU_GO,          // monthlyFee=0
    MONTHLY_SUBSCRIPTION,   // monthlyFee>0
//    PREPAID,                // monthlyFee=0 & >=1 usage price>0
//    POSTPAID,               // monthlyFee=0 & >=1 usage price>0
    VIP                     // monthlyFee>0
}
