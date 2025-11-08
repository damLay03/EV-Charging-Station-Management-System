# Wallet API Documentation

## Overview
The Wallet feature provides an in-app wallet system for drivers to manage their funds. All payments for charging sessions and deposits are made through the wallet. Users can top-up their wallet using ZaloPay or cash (via staff).

## Base URL
```
/api/wallet
```

---

## Endpoints

### 1. Get Wallet Balance
Retrieve the current wallet balance for the authenticated driver.

**Endpoint:** `GET /api/wallet/balance`

**Authentication:** Required (DRIVER role)

**Response:**
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "walletId": 1,
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "balance": 500000.0,
    "updatedAt": "2025-11-09T10:30:00"
  }
}
```

---

### 2. Get Transaction History
Retrieve the transaction history for the authenticated driver's wallet.

**Endpoint:** `GET /api/wallet/history`

**Authentication:** Required (DRIVER role)

**Response:**
```json
{
  "code": 1000,
  "message": null,
  "result": [
    {
      "id": 1,
      "amount": 100000.0,
      "transactionType": "TOPUP_ZALOPAY",
      "status": "COMPLETED",
      "timestamp": "2025-11-09T10:00:00",
      "description": "Top-up via ZaloPay",
      "externalTransactionId": "251109_12345",
      "processedByStaffId": null,
      "processedByStaffName": null,
      "relatedBookingId": null,
      "relatedSessionId": null
    },
    {
      "id": 2,
      "amount": -50000.0,
      "transactionType": "CHARGING_PAYMENT",
      "status": "COMPLETED",
      "timestamp": "2025-11-09T11:00:00",
      "description": "Payment for charging session",
      "externalTransactionId": null,
      "processedByStaffId": null,
      "processedByStaffName": null,
      "relatedBookingId": null,
      "relatedSessionId": "abc123"
    }
  ]
}
```

**Transaction Types:**
- `TOPUP_ZALOPAY` - Top-up via ZaloPay
- `TOPUP_CASH` - Cash top-up via staff
- `BOOKING_DEPOSIT` - Booking deposit deduction
- `BOOKING_DEPOSIT_REFUND` - Booking deposit refund
- `CHARGING_PAYMENT` - Charging session payment
- `ADMIN_ADJUSTMENT` - Admin adjustment

**Transaction Status:**
- `PENDING` - Transaction is pending
- `COMPLETED` - Transaction completed successfully
- `FAILED` - Transaction failed

---

### 3. Create ZaloPay Top-up
Create a ZaloPay order to top-up the wallet.

**Endpoint:** `POST /api/wallet/topup/zalopay`

**Authentication:** Required (DRIVER role)

**Request Body:**
```json
{
  "amount": 100000.0
}
```

**Validation:**
- `amount`: Required, must be positive

**Response:**
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "orderUrl": "https://sbgateway.zalopay.vn/openinapp?order=...",
    "appTransId": "251109_12345",
    "transactionId": 1,
    "message": "Top-up order created successfully"
  }
}
```

**Flow:**
1. Driver sends request with amount
2. System creates pending wallet transaction
3. System calls ZaloPay API to create payment order
4. System returns `orderUrl` for driver to complete payment
5. After payment, ZaloPay calls webhook to confirm
6. System updates transaction status and credits wallet

---

### 4. Process Cash Top-up (Staff Only)
Process a cash top-up for a driver.

**Endpoint:** `POST /api/wallet/topup/cash`

**Authentication:** Required (STAFF role)

**Request Body:**
```json
{
  "targetUserIdentifier": "user@example.com",
  "amount": 100000.0,
  "description": "Cash top-up at Station A"
}
```

**Validation:**
- `targetUserIdentifier`: Required - Email or phone number of the target user
- `amount`: Required, must be positive
- `description`: Optional

**Response:**
```json
{
  "code": 1000,
  "message": null,
  "result": {
    "id": 1,
    "amount": 100000.0,
    "transactionType": "TOPUP_CASH",
    "status": "COMPLETED",
    "timestamp": "2025-11-09T10:00:00",
    "description": "Cash top-up at Station A",
    "externalTransactionId": null,
    "processedByStaffId": "staff-123",
    "processedByStaffName": "John Doe",
    "relatedBookingId": null,
    "relatedSessionId": null
  }
}
```

---

## Webhook Endpoints

### ZaloPay Top-up Callback
**Endpoint:** `POST /api/webhooks/zalopay/topup`

**Authentication:** None (Called by ZaloPay)

**Purpose:** Receives payment confirmation from ZaloPay and credits the user's wallet.

**Request Body:**
```json
{
  "data": "{...encrypted data...}",
  "mac": "MAC_signature"
}
```

**Response:**
```json
{
  "return_code": 1,
  "return_message": "Success"
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 19001 | Wallet Not Found | User doesn't have a wallet |
| 19002 | Insufficient Funds In Wallet | Not enough balance for transaction |
| 19003 | Wallet Transaction Not Found | Transaction not found |
| 19004 | Invalid Top-up Amount | Amount must be positive |
| 19005 | Wallet Already Exists For This User | Cannot create duplicate wallet |
| 1004 | User Not Found | Target user not found |
| 4001 | Staff Not Found | Staff member not found |
| 17000 | ZaloPay API error | Error communicating with ZaloPay |

---

## Integration Guide

### For Frontend Developers

#### 1. Display Wallet Balance
```javascript
// Get wallet balance
const response = await fetch('/api/wallet/balance', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const data = await response.json();
console.log('Balance:', data.result.balance);
```

#### 2. Top-up via ZaloPay
```javascript
// Create top-up order
const response = await fetch('/api/wallet/topup/zalopay', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ amount: 100000 })
});
const data = await response.json();

// Redirect user to ZaloPay payment page
window.location.href = data.result.orderUrl;
```

#### 3. View Transaction History
```javascript
const response = await fetch('/api/wallet/history', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
});
const data = await response.json();
console.log('Transactions:', data.result);
```

### For Staff Application

#### Process Cash Top-up
```javascript
const response = await fetch('/api/wallet/topup/cash', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${staffToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    targetUserIdentifier: 'driver@example.com',
    amount: 100000,
    description: 'Cash payment at counter'
  })
});
const data = await response.json();
console.log('Transaction:', data.result);
```

---

## Database Schema

### Table: wallets
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Wallet ID |
| user_id | VARCHAR (FK) | User ID (One-to-One with users) |
| balance | DOUBLE | Current balance |
| updated_at | TIMESTAMP | Last update timestamp |

### Table: wallet_transactions
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT (PK) | Transaction ID |
| wallet_id | BIGINT (FK) | Wallet ID |
| amount | DOUBLE | Transaction amount (negative for debit) |
| transaction_type | VARCHAR | Type of transaction |
| status | VARCHAR | Transaction status |
| timestamp | TIMESTAMP | Transaction timestamp |
| description | VARCHAR | Transaction description |
| external_transaction_id | VARCHAR | External payment gateway transaction ID |
| processed_by_staff_id | VARCHAR (FK) | Staff who processed the transaction |
| related_booking_id | BIGINT | Related booking ID (if applicable) |
| related_session_id | VARCHAR | Related charging session ID (if applicable) |

---

## Business Rules

1. **Automatic Wallet Creation**: A wallet is automatically created when a user registers as a DRIVER.

2. **Wallet Balance**: 
   - Cannot go negative
   - All transactions are recorded in wallet_transactions
   - Positive amounts = credits (top-up, refunds)
   - Negative amounts = debits (payments, deposits)

3. **Payment Flow**:
   - All service payments (charging, deposits) are deducted from the wallet
   - Users must top-up if balance is insufficient
   - Payment methods (ZaloPay, Cash) are only for wallet top-up

4. **Transaction Status**:
   - PENDING: Transaction created but not confirmed
   - COMPLETED: Transaction successful and balance updated
   - FAILED: Transaction failed, balance not affected

5. **Staff Cash Top-up**:
   - Only staff members can process cash top-ups
   - Staff must identify user by email or phone
   - Transaction is recorded with staff's ID

---

## Testing

### Test Scenarios

1. **Driver Registration**: Verify wallet is created automatically
2. **Get Balance**: Verify driver can view their balance
3. **ZaloPay Top-up**: Test complete flow including webhook callback
4. **Cash Top-up**: Staff can top-up for a driver
5. **Insufficient Funds**: Verify error when trying to pay with low balance
6. **Transaction History**: Verify all transactions are recorded correctly

### Sample Test Data

```sql
-- Insert test wallet
INSERT INTO wallets (user_id, balance, updated_at) 
VALUES ('test-driver-id', 500000, NOW());

-- Insert test transactions
INSERT INTO wallet_transactions (wallet_id, amount, transaction_type, status, timestamp, description)
VALUES 
  (1, 100000, 'TOPUP_ZALOPAY', 'COMPLETED', NOW(), 'Test top-up'),
  (1, -50000, 'CHARGING_PAYMENT', 'COMPLETED', NOW(), 'Test payment');
```

