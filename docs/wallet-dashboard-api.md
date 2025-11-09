# Wallet Dashboard API Documentation

## APIs cho Frontend Wallet Dashboard

### 1. GET /api/wallet/dashboard - Lấy Dashboard Overview

**Mô tả:** Lấy tổng quan ví bao gồm số dư hiện tại và thống kê tháng này.

**Authentication:** Required (DRIVER role)

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "currentBalance": 1250000.0,
    "statistics": {
      "monthlySpending": 70000.0,
      "monthlyTopUp": 1500000.0,
      "transactionCount": 8
    }
  }
}
```

**Mapping với UI:**
- `currentBalance` → Số dư ví chính (1.250.000 VNĐ)
- `statistics.monthlySpending` → Chi tiêu tháng này (70.000 VNĐ)
- `statistics.monthlyTopUp` → Nạp tháng này (1.500.000 VNĐ)
- `statistics.transactionCount` → Số giao dịch (8 lần)

---

### 2. GET /api/wallet/balance - Lấy Số Dư Ví

**Mô tả:** Chỉ lấy số dư ví hiện tại (đã có sẵn).

**Authentication:** Required (DRIVER role)

**Response:**
```json
{
  "code": 1000,
  "result": {
    "walletId": 1,
    "userId": "USER123",
    "balance": 1250000.0,
    "updatedAt": "2025-11-09T14:30:00"
  }
}
```

---

### 3. GET /api/wallet/history - Lấy Lịch Sử Giao Dịch (có filter)

**Mô tả:** Lấy danh sách giao dịch với khả năng filter theo loại.

**Authentication:** Required (DRIVER role)

**Query Parameters:**
- `type` (optional): Filter theo loại giao dịch
  - `ALL` hoặc null: Tất cả giao dịch
  - `TOPUP` hoặc `NAP_TIEN`: Chỉ giao dịch nạp tiền
  - `CHARGING` hoặc `SAC_XE`: Chỉ giao dịch sạc xe
  - `REFUND` hoặc `HOAN_TIEN`: Chỉ giao dịch hoàn tiền

**Request Examples:**
```bash
# Lấy tất cả giao dịch
GET /api/wallet/history

# Lấy chỉ giao dịch nạp tiền
GET /api/wallet/history?type=TOPUP

# Lấy chỉ giao dịch sạc xe
GET /api/wallet/history?type=CHARGING

# Lấy chỉ giao dịch hoàn tiền
GET /api/wallet/history?type=REFUND
```

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "id": 1,
      "amount": 500000.0,
      "transactionType": "TOPUP_ZALOPAY",
      "status": "COMPLETED",
      "timestamp": "2025-11-09T14:30:00",
      "description": "Nạp tiền vào ví",
      "externalTransactionId": "251109_123456",
      "processedByStaffId": null,
      "processedByStaffName": null,
      "relatedBookingId": null,
      "relatedSessionId": null
    },
    {
      "id": 2,
      "amount": -45000.0,
      "transactionType": "CHARGING_PAYMENT",
      "status": "COMPLETED",
      "timestamp": "2025-11-08T10:00:00",
      "description": "Sạc tại Trạm VinCity",
      "externalTransactionId": null,
      "processedByStaffId": null,
      "processedByStaffName": null,
      "relatedBookingId": null,
      "relatedSessionId": "SESSION123"
    }
  ]
}
```

**Lưu ý về amount:**
- **Dương (+)**: Nạp tiền vào ví (màu xanh lá)
- **Âm (-)**: Trừ tiền từ ví (màu đỏ/xanh dương)

---

### 4. POST /api/wallet/topup/zalopay - Nạp Tiền Qua ZaloPay

**Mô tả:** Tạo order nạp tiền qua ZaloPay (đã có sẵn).

**Authentication:** Required (DRIVER role)

**Request:**
```json
{
  "amount": 500000
}
```

**Response:**
```json
{
  "code": 1000,
  "result": {
    "orderUrl": "https://sb-openapi.zalopay.vn/v2/gateway?order=...",
    "appTransId": "251109_123456",
    "transactionId": 123,
    "message": "Top-up order created successfully"
  }
}
```

**Flow:**
1. Frontend gọi API này
2. Backend trả về `orderUrl`
3. Frontend redirect user đến `orderUrl`
4. User thanh toán trên ZaloPay
5. ZaloPay callback về backend
6. Backend cập nhật số dư ví

---

## Mapping Frontend Components

### 1. Wallet Card (Số dư ví chính)
```typescript
// Gọi API
GET /api/wallet/dashboard

// Hiển thị
<div className="wallet-card">
  <h2>{dashboard.currentBalance.toLocaleString()} VNĐ</h2>
  <button onClick={handleTopUp}>+ Nạp tiền</button>
</div>
```

### 2. Statistics Cards (Chi tiêu, Nạp, Giao dịch)
```typescript
// Data từ dashboard.statistics
<div className="stats">
  <StatCard 
    title="Chi tiêu tháng này"
    value={dashboard.statistics.monthlySpending}
    icon="chart"
  />
  <StatCard 
    title="Nạp tháng này"
    value={dashboard.statistics.monthlyTopUp}
    icon="money"
  />
  <StatCard 
    title="Giao dịch"
    value={dashboard.statistics.transactionCount}
    unit="lần"
    icon="receipt"
  />
</div>
```

### 3. Transaction History với Tabs
```typescript
const [filterType, setFilterType] = useState('ALL');

// Gọi API khi thay đổi tab
useEffect(() => {
  fetchHistory(filterType);
}, [filterType]);

const fetchHistory = async (type) => {
  const url = type === 'ALL' 
    ? '/api/wallet/history'
    : `/api/wallet/history?type=${type}`;
  const response = await fetch(url);
  // ...
};

// Tabs
<Tabs>
  <Tab onClick={() => setFilterType('ALL')}>Tất cả</Tab>
  <Tab onClick={() => setFilterType('TOPUP')}>Nạp tiền</Tab>
  <Tab onClick={() => setFilterType('CHARGING')}>Sạc xe</Tab>
  <Tab onClick={() => setFilterType('REFUND')}>Hoàn tiền</Tab>
</Tabs>
```

### 4. Transaction Item
```typescript
<TransactionItem>
  <Icon type={getTransactionIcon(transaction.transactionType)} />
  <div>
    <h4>{transaction.description}</h4>
    <p>{formatDate(transaction.timestamp)}</p>
  </div>
  <div className={transaction.amount > 0 ? 'positive' : 'negative'}>
    <span>{transaction.amount > 0 ? '+' : ''}</span>
    <span>{Math.abs(transaction.amount).toLocaleString()} VNĐ</span>
  </div>
  <Badge status={transaction.status} />
</TransactionItem>
```

---

## Transaction Types Mapping

| Transaction Type | Hiển thị | Icon | Màu | Filter Tab |
|-----------------|----------|------|-----|------------|
| `TOPUP_ZALOPAY` | Nạp tiền vào ví | 💰 | Xanh lá | Nạp tiền |
| `TOPUP_CASH` | Nạp tiền mặt | 💵 | Xanh lá | Nạp tiền |
| `CHARGING_PAYMENT` | Sạc tại [Station] | ⚡ | Đỏ | Sạc xe |
| `BOOKING_DEPOSIT` | Đặt cọc trụ sạc | 🔒 | Vàng | - |
| `BOOKING_DEPOSIT_REFUND` | Hoàn tiền đặt cọc | ↩️ | Xanh lá | Hoàn tiền |

---

## Status Colors

| Status | Hiển thị | Màu |
|--------|----------|-----|
| `COMPLETED` | Hoàn thành | Xanh lá |
| `PENDING` | Đang xử lý | Vàng |
| `FAILED` | Thất bại | Đỏ |

---

## Example Frontend Code (React)

```typescript
// hooks/useWalletDashboard.ts
export const useWalletDashboard = () => {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchDashboard();
  }, []);

  const fetchDashboard = async () => {
    try {
      const response = await fetch('/api/wallet/dashboard', {
        headers: {
          'Authorization': `Bearer ${getToken()}`
        }
      });
      const data = await response.json();
      setDashboard(data.result);
    } catch (error) {
      console.error('Error fetching dashboard:', error);
    } finally {
      setLoading(false);
    }
  };

  return { dashboard, loading, refresh: fetchDashboard };
};

// components/WalletDashboard.tsx
export const WalletDashboard = () => {
  const { dashboard, loading } = useWalletDashboard();
  const [filterType, setFilterType] = useState('ALL');
  const [history, setHistory] = useState([]);

  const fetchHistory = async (type) => {
    const url = type === 'ALL' 
      ? '/api/wallet/history'
      : `/api/wallet/history?type=${type}`;
    
    const response = await fetch(url, {
      headers: { 'Authorization': `Bearer ${getToken()}` }
    });
    const data = await response.json();
    setHistory(data.result);
  };

  useEffect(() => {
    fetchHistory(filterType);
  }, [filterType]);

  if (loading) return <Spinner />;

  return (
    <div className="wallet-dashboard">
      {/* Wallet Card */}
      <WalletCard balance={dashboard.currentBalance} />
      
      {/* Statistics */}
      <div className="statistics">
        <StatCard
          title="Chi tiêu tháng này"
          value={dashboard.statistics.monthlySpending}
          icon={<TrendingDown />}
        />
        <StatCard
          title="Nạp tháng này"
          value={dashboard.statistics.monthlyTopUp}
          icon={<TrendingUp />}
        />
        <StatCard
          title="Giao dịch"
          value={dashboard.statistics.transactionCount}
          unit="lần"
          icon={<Receipt />}
        />
      </div>

      {/* Transaction History */}
      <div className="transaction-history">
        <h3>Lịch sử giao dịch</h3>
        <Tabs value={filterType} onChange={setFilterType}>
          <Tab value="ALL">Tất cả</Tab>
          <Tab value="TOPUP">Nạp tiền</Tab>
          <Tab value="CHARGING">Sạc xe</Tab>
          <Tab value="REFUND">Hoàn tiền</Tab>
        </Tabs>
        
        <TransactionList transactions={history} />
      </div>
    </div>
  );
};
```

---

## Testing với Postman

### 1. Get Dashboard
```
GET {{baseUrl}}/api/wallet/dashboard
Authorization: Bearer {{token}}
```

### 2. Get History - All
```
GET {{baseUrl}}/api/wallet/history
Authorization: Bearer {{token}}
```

### 3. Get History - Top-up only
```
GET {{baseUrl}}/api/wallet/history?type=TOPUP
Authorization: Bearer {{token}}
```

### 4. Get History - Charging only
```
GET {{baseUrl}}/api/wallet/history?type=CHARGING
Authorization: Bearer {{token}}
```

---

## Notes

1. **Caching**: Frontend nên cache dashboard data và chỉ refresh khi có transaction mới
2. **Real-time**: Có thể implement WebSocket để update real-time khi có transaction mới
3. **Pagination**: Nếu history quá dài, cần thêm pagination (limit, offset)
4. **Date Range**: Có thể thêm filter theo khoảng thời gian (startDate, endDate)
5. **Pull to Refresh**: Implement pull-to-refresh trên mobile để cập nhật số dư

**Tất cả API đã sẵn sàng cho Frontend sử dụng!** 🎉

