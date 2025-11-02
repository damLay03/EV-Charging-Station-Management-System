# Payment History API

API để xem lịch sử thanh toán của một trạm sạc.

## Endpoint

```
GET /api/stations/{stationId}/payment-history
```

## Phân quyền

- **ADMIN**: Có thể xem lịch sử của bất kỳ trạm nào
- **STAFF**: Chỉ được xem lịch sử của trạm mình quản lý

## Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| stationId | String (Path) | Yes | ID của trạm cần xem lịch sử |
| startDate | LocalDate (Query) | No | Ngày bắt đầu filter (format: yyyy-MM-dd) |
| endDate | LocalDate (Query) | No | Ngày kết thúc filter (format: yyyy-MM-dd) |
| paymentMethod | Enum (Query) | No | Phương thức thanh toán: `CASH`, `ZALOPAY` |

## Response

### Success Response (200 OK)

```json
{
  "code": 1000,
  "result": [
    {
      "paymentId": "PAY123",
      "paymentTime": "2024-11-02T15:45:00",
      "chargingPointName": "Điểm sạc TS2",
      "customerName": "Lê Văn Cường",
      "durationFormatted": "35 phút",
      "durationMinutes": 35,
      "amount": 65000.0,
      "paymentMethod": "ZALOPAY",
      "paymentMethodDisplay": "ZaloPay",
      "sessionId": "SESSION123"
    },
    {
      "paymentId": "PAY124",
      "paymentTime": "2024-11-02T14:20:00",
      "chargingPointName": "Điểm sạc TS1",
      "customerName": "Phạm Thị Dung",
      "durationFormatted": "1h 15m",
      "durationMinutes": 75,
      "amount": 120000.0,
      "paymentMethod": "CASH",
      "paymentMethodDisplay": "Tiền mặt",
      "sessionId": "SESSION124"
    },
    {
      "paymentId": "PAY125",
      "paymentTime": "2024-11-02T13:30:00",
      "chargingPointName": "Điểm sạc TS5",
      "customerName": "Hoàng Minh Tài",
      "durationFormatted": "50 phút",
      "durationMinutes": 50,
      "amount": 95000.0,
      "paymentMethod": "ZALOPAY",
      "paymentMethodDisplay": "ZaloPay",
      "sessionId": "SESSION125"
    }
  ]
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| paymentId | String | ID của payment |
| paymentTime | LocalDateTime | Thời gian thanh toán (đã hoàn thành) |
| chargingPointName | String | Tên điểm sạc (format: "Điểm sạc TS{n}") |
| customerName | String | Tên khách hàng |
| durationFormatted | String | Thời gian sạc dạng text ("35 phút", "1h 15m") |
| durationMinutes | Integer | Thời gian sạc (phút) |
| amount | Float | Số tiền thanh toán |
| paymentMethod | Enum | Phương thức thanh toán enum: CASH, ZALOPAY |
| paymentMethodDisplay | String | Tên hiển thị PT thanh toán ("Tiền mặt", "ZaloPay") |
| sessionId | String | ID của charging session |

## Example Requests

### 1. Lấy tất cả lịch sử thanh toán của trạm

```bash
GET /api/stations/ST001/payment-history
```

### 2. Lọc theo ngày

```bash
GET /api/stations/ST001/payment-history?startDate=2024-11-01&endDate=2024-11-02
```

### 3. Lọc theo phương thức thanh toán

```bash
GET /api/stations/ST001/payment-history?paymentMethod=CASH
```

### 4. Lọc kết hợp

```bash
GET /api/stations/ST001/payment-history?startDate=2024-11-01&endDate=2024-11-02&paymentMethod=ZALOPAY
```

## Error Responses

### Station Not Found (404)

```json
{
  "code": 1008,
  "message": "Station not found"
}
```

### Unauthorized (403)

```json
{
  "code": 1003,
  "message": "You do not have permission"
}
```

## Notes

- Chỉ trả về các payment có status = `COMPLETED`
- Kết quả được sắp xếp theo `paidAt` DESC (mới nhất trước)
- Nếu không truyền filter, trả về tất cả lịch sử
- Duration được format tự động: 
  - < 60 phút: "X phút"
  - >= 60 phút: "Xh Ym"
  - Chỉ giờ: "Xh"

## Implementation Details

- **Repository**: `PaymentRepository.findPaymentHistoryByStationId()`
- **Service**: `StationService.getPaymentHistory()`
- **Controller**: `StationController.getPaymentHistory()`
- **DTO**: `PaymentHistoryResponse`

