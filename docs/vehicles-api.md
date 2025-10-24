# Vehicle Management API Documentation

## Tổng quan về refactoring

### Những thay đổi chính:

1. **Loại bỏ trùng lặp dữ liệu trong Vehicle Entity**
   - ❌ Trước: `brand`, `batteryCapacityKwh`, `batteryType` được lưu trực tiếp trong entity
   - ✅ Sau: Chỉ lưu `model` (enum), các thông tin khác được lấy tự động từ `VehicleModel` enum
   - **Lợi ích**: Single Source of Truth, đảm bảo dữ liệu luôn đồng bộ với enum

2. **Đơn giản hóa Request DTOs**
   - ❌ Trước: User phải chọn cả `brand` và `model`, backend validate model có thuộc brand không
   - ✅ Sau: User chỉ cần chọn `model`, `brand` tự động được xác định
   - **Lợi ích**: UX tốt hơn, ít lỗi validation, logic rõ ràng hơn

3. **Tối ưu Response DTO**
   - ❌ Trước: Trả về nhiều field trùng lặp
   - ✅ Sau: Chỉ lưu core fields, các thông tin khác computed qua getter methods
   - **Lợi ích**: Response nhẹ hơn, dễ maintain

---

## API Endpoints

### 1. PUBLIC APIs (Không cần authentication)

#### 1.1. Lấy danh sách tất cả hãng xe
```http
GET /api/vehicles/brands
```

**Response:**
```json
{
  "code": 200,
  "result": [
    {
      "brand": "VINFAST",
      "displayName": "VinFast",
      "country": "Việt Nam"
    },
    {
      "brand": "TESLA",
      "displayName": "Tesla",
      "country": "Mỹ"
    },
    {
      "brand": "BYD",
      "displayName": "BYD",
      "country": "Trung Quốc"
    }
  ]
}
```

#### 1.2. Lấy danh sách models theo brand
```http
GET /api/vehicles/brands/{brand}/models
```

**Example:**
```http
GET /api/vehicles/brands/VINFAST/models
```

**Response:**
```json
{
  "code": 200,
  "result": [
    {
      "model": "VINFAST_VF5",
      "modelName": "VF5",
      "brand": "VINFAST",
      "batteryCapacityKwh": 37.23,
      "batteryType": "LFP (Lithium Iron Phosphate)"
    },
    {
      "model": "VINFAST_VF8",
      "modelName": "VF8",
      "brand": "VINFAST",
      "batteryCapacityKwh": 87.7,
      "batteryType": "NMC (Nickel Manganese Cobalt)"
    }
  ]
}
```

#### 1.3. Lấy danh sách tất cả models
```http
GET /api/vehicles/models
```

**Response:**
```json
{
  "code": 200,
  "result": [
    {
      "model": "VINFAST_VF5",
      "modelName": "VF5",
      "brand": "VINFAST",
      "batteryCapacityKwh": 37.23,
      "batteryType": "LFP (Lithium Iron Phosphate)"
    },
    {
      "model": "TESLA_MODEL_3",
      "modelName": "Model 3 Standard Range",
      "brand": "TESLA",
      "batteryCapacityKwh": 60.0,
      "batteryType": "LFP (Lithium Iron Phosphate)"
    }
  ]
}
```

---

### 2. DRIVER APIs (Role: DRIVER)

#### 2.1. Tạo xe mới
```http
POST /api/vehicles
Authorization: Bearer {token}
```

**Request Body:**
```json
{
  "licensePlate": "30A-12345",
  "model": "VINFAST_VF8"
}
```

**⚠️ Lưu ý:**
- Không cần gửi `brand` - backend tự động xác định từ `model`
- Không cần gửi `batteryCapacityKwh`, `batteryType` - tự động lấy từ enum

**Response:**
```json
{
  "code": 200,
  "result": {
    "vehicleId": "uuid-123",
    "licensePlate": "30A-12345",
    "model": "VINFAST_VF8",
    "ownerId": "driver-uuid",
    "brand": "VINFAST",
    "brandDisplayName": "VinFast",
    "modelName": "VF8",
    "batteryCapacityKwh": 87.7,
    "batteryType": "NMC (Nickel Manganese Cobalt)"
  }
}
```

#### 2.2. Lấy danh sách xe của tôi
```http
GET /api/vehicles/my-vehicles
Authorization: Bearer {token}
```

**Response:**
```json
{
  "code": 200,
  "result": [
    {
      "vehicleId": "uuid-123",
      "licensePlate": "30A-12345",
      "model": "VINFAST_VF8",
      "ownerId": "driver-uuid",
      "brand": "VINFAST",
      "brandDisplayName": "VinFast",
      "modelName": "VF8",
      "batteryCapacityKwh": 87.7,
      "batteryType": "NMC (Nickel Manganese Cobalt)"
    }
  ]
}
```

#### 2.3. Lấy chi tiết một xe
```http
GET /api/vehicles/my-vehicles/{vehicleId}
Authorization: Bearer {token}
```

#### 2.4. Cập nhật thông tin xe
```http
PUT /api/vehicles/{vehicleId}
Authorization: Bearer {token}
```

**Request Body (tất cả fields đều optional):**
```json
{
  "licensePlate": "30A-99999",
  "model": "VINFAST_VF9"
}
```

**⚠️ Lưu ý:**
- Khi đổi `model`, `brand` sẽ tự động thay đổi theo
- Ví dụ: Đổi từ `VINFAST_VF8` → `TESLA_MODEL_3` thì brand tự động đổi từ VINFAST → TESLA

**Response:**
```json
{
  "code": 200,
  "result": {
    "vehicleId": "uuid-123",
    "licensePlate": "30A-99999",
    "model": "VINFAST_VF9",
    "ownerId": "driver-uuid",
    "brand": "VINFAST",
    "brandDisplayName": "VinFast",
    "modelName": "VF9",
    "batteryCapacityKwh": 123.0,
    "batteryType": "NMC (Nickel Manganese Cobalt)"
  }
}
```

#### 2.5. Xóa xe
```http
DELETE /api/vehicles/{vehicleId}
Authorization: Bearer {token}
```

**Response:**
```json
{
  "code": 200,
  "message": "Vehicle deleted successfully"
}
```

---

### 3. ADMIN APIs (Role: ADMIN)

#### 3.1. Lấy danh sách xe của một driver
```http
GET /api/vehicles/driver/{driverId}
Authorization: Bearer {admin-token}
```

---

## Database Schema Changes

### Trước refactoring:
```sql
CREATE TABLE vehicles (
  vehicle_id VARCHAR(255) PRIMARY KEY,
  license_plate VARCHAR(255) UNIQUE,
  brand VARCHAR(50),              -- ❌ Redundant
  model VARCHAR(50),
  battery_capacity_kwh FLOAT,     -- ❌ Redundant
  battery_type VARCHAR(100),      -- ❌ Redundant
  current_soc_percent INT,
  owner_id VARCHAR(255)
);
```

### Sau refactoring:
```sql
CREATE TABLE vehicles (
  vehicle_id VARCHAR(255) PRIMARY KEY,
  license_plate VARCHAR(255) UNIQUE,
  model VARCHAR(50),              -- ✅ Enum: VINFAST_VF8, TESLA_MODEL_3, etc.
  current_soc_percent INT,
  owner_id VARCHAR(255)
);
```

**⚠️ Migration Notes:**
- Các cột `brand`, `battery_capacity_kwh`, `battery_type` sẽ tự động bị xóa bởi Hibernate (ddl-auto: update)
- Dữ liệu cũ vẫn an toàn vì chỉ cần `model` là đủ để tái tạo toàn bộ thông tin

---

## Frontend Integration Guide

### Workflow tạo xe mới (2-step selection):

**Bước 1: User chọn hãng xe**

1. **Frontend hiển thị dropdown "Chọn hãng xe"**
   ```javascript
   // Gọi API lấy danh sách brands
   GET /api/vehicles/brands
   
   // Response:
   [
     { brand: "VINFAST", displayName: "VinFast", country: "Việt Nam" },
     { brand: "TESLA", displayName: "Tesla", country: "Mỹ" },
     { brand: "BYD", displayName: "BYD", country: "Trung Quốc" }
   ]
   ```

2. **User chọn brand** (ví dụ: chọn "VinFast")

**Bước 2: User chọn model xe**

3. **Frontend gọi API lấy models của brand đã chọn**
   ```javascript
   // Khi user chọn VINFAST, gọi:
   GET /api/vehicles/brands/VINFAST/models
   
   // Response:
   [
     { 
       model: "VINFAST_VF5", 
       modelName: "VF5", 
       brand: "VINFAST",
       batteryCapacityKwh: 37.23,
       batteryType: "LFP (Lithium Iron Phosphate)"
     },
     { 
       model: "VINFAST_VF8", 
       modelName: "VF8", 
       brand: "VINFAST",
       batteryCapacityKwh: 87.7,
       batteryType: "NMC (Nickel Manganese Cobalt)"
     },
     // ... các models khác của VinFast
   ]
   ```

4. **Frontend hiển thị dropdown "Chọn model xe"** với danh sách models của brand đã chọn
   - Có thể hiển thị thêm thông tin battery capacity và battery type để user dễ lựa chọn

5. **User chọn model** (ví dụ: chọn "VF8")

**Bước 3: Submit form**

6. **Gửi request tạo xe** - CHỈ GỬI `licensePlate` và `model` (không cần gửi brand)
   ```javascript
   POST /api/vehicles
   {
     "licensePlate": "30A-12345",
     "model": "VINFAST_VF8"  // ← Backend tự hiểu brand là VINFAST
   }
   ```

**✨ Lợi ích của UX flow này:**
- ✅ User dễ dàng tìm model xe của mình (không bị overwhelm bởi quá nhiều models)
- ✅ Dropdown thứ 2 chỉ hiển thị models liên quan đến brand đã chọn
- ✅ Backend validation đơn giản (không cần kiểm tra brand/model có match không)
- ✅ Không thể chọn nhầm model của brand khác

---

### UI Mockup Example:

```
┌─────────────────────────────────────────┐
│  Thêm xe mới                            │
├─────────────────────────────────────────┤
│                                         │
│  Biển số xe: [30A-12345_______]        │
│                                         │
│  Hãng xe: [VinFast ▼]                  │
│           ├─ VinFast ✓                 │
│           ├─ Tesla                      │
│           └─ BYD                        │
│                                         │
│  Model xe: [VF8 ▼]                     │
│            ├─ VF5 (37.23 kWh, LFP)     │
│            ├─ VF6 (59.6 kWh, LFP)      │
│            ├─ VF7 (75.3 kWh, NMC)      │
│            ├─ VF8 (87.7 kWh, NMC) ✓    │
│            ├─ VF9 (123.0 kWh, NMC)     │
│            └─ VF e34 (42.0 kWh, LFP)   │
│                                         │
│  [Hủy]              [Thêm xe]          │
└─────────────────────────────────────────┘
```

---

### React/Vue Example Code:

```javascript
// React example
const [brands, setBrands] = useState([]);
const [selectedBrand, setSelectedBrand] = useState(null);
const [models, setModels] = useState([]);
const [selectedModel, setSelectedModel] = useState(null);
const [licensePlate, setLicensePlate] = useState('');

// Step 1: Load brands on component mount
useEffect(() => {
  fetch('/api/vehicles/brands')
    .then(res => res.json())
    .then(data => setBrands(data.result));
}, []);

// Step 2: Load models when brand is selected
useEffect(() => {
  if (selectedBrand) {
    fetch(`/api/vehicles/brands/${selectedBrand}/models`)
      .then(res => res.json())
      .then(data => setModels(data.result));
  }
}, [selectedBrand]);

// Step 3: Submit form
const handleSubmit = async () => {
  const response = await fetch('/api/vehicles', {
    method: 'POST',
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      licensePlate: licensePlate,
      model: selectedModel  // Only send model, not brand!
    })
  });
  
  const data = await response.json();
  console.log('Created vehicle:', data.result);
};
```

---

### Hiển thị thông tin xe:

Response từ API đã bao gồm tất cả thông tin cần thiết:
```javascript
const vehicle = response.result;

// Các field có sẵn:
vehicle.vehicleId
vehicle.licensePlate
vehicle.model              // Enum: "VINFAST_VF8"
vehicle.ownerId

// Các field computed (getter methods):
vehicle.brand              // Enum: "VINFAST"
vehicle.brandDisplayName   // String: "VinFast"
vehicle.modelName          // String: "VF8"
vehicle.batteryCapacityKwh // Float: 87.7
vehicle.batteryType        // String: "NMC (Nickel Manganese Cobalt)"
```

**💡 Tip cho Frontend:**
Khi hiển thị danh sách xe, bạn có thể show:
```
VinFast VF8
30A-12345 | 87.7 kWh | NMC
```

---

## Available Models

### VinFast:
- `VINFAST_VF5` - VF5 (37.23 kWh, LFP)
- `VINFAST_VF6` - VF6 (59.6 kWh, LFP)
- `VINFAST_VF7` - VF7 (75.3 kWh, NMC)
- `VINFAST_VF8` - VF8 (87.7 kWh, NMC)
- `VINFAST_VF9` - VF9 (123.0 kWh, NMC)
- `VINFAST_VFE34` - VF e34 (42.0 kWh, LFP)

### Tesla:
- `TESLA_MODEL_3` - Model 3 Standard Range (60.0 kWh, LFP)
- `TESLA_MODEL_3_LONG_RANGE` - Model 3 Long Range (82.0 kWh, NCA)
- `TESLA_MODEL_Y` - Model Y Standard Range (60.0 kWh, LFP)
- `TESLA_MODEL_Y_LONG_RANGE` - Model Y Long Range (82.0 kWh, NCA)
- `TESLA_MODEL_S` - Model S (100.0 kWh, NCA)
- `TESLA_MODEL_X` - Model X (100.0 kWh, NCA)

### BYD:
- `BYD_ATTO_3` - Atto 3 (60.48 kWh, Blade Battery LFP)
- `BYD_DOLPHIN` - Dolphin (44.9 kWh, Blade Battery LFP)
- `BYD_SEAL` - Seal (82.56 kWh, Blade Battery LFP)
- `BYD_HAN` - Han EV (85.44 kWh, Blade Battery LFP)
- `BYD_TANG` - Tang EV (108.8 kWh, Blade Battery LFP)
- `BYD_YUAN_PLUS` - Yuan Plus (50.12 kWh, Blade Battery LFP)

---

## Error Codes

| Code | Error | Description |
|------|-------|-------------|
| 5001 | `VEHICLE_NOT_FOUND` | Không tìm thấy xe |
| 5002 | `LICENSE_PLATE_EXISTED` | Biển số xe đã tồn tại |
| 5003 | `VEHICLE_NOT_BELONG_TO_DRIVER` | Xe không thuộc về driver này |

**Đã loại bỏ:**
- ❌ `INVALID_VEHICLE_MODEL_FOR_BRAND` - Không còn cần vì brand tự động xác định từ model

---

## Benefits of New Architecture

1. ✅ **Single Source of Truth**: Tất cả thông tin về model (brand, battery, etc.) chỉ định nghĩa 1 lần trong enum
2. ✅ **Data Consistency**: Không thể có trường hợp brand/model không khớp với battery specs
3. ✅ **Easier Maintenance**: Thêm model mới chỉ cần update enum, không cần migration
4. ✅ **Better UX**: User chỉ cần chọn model, không lo lắng về brand
5. ✅ **Smaller Payload**: Response nhẹ hơn, database nhỏ hơn
6. ✅ **Type Safety**: Enum đảm bảo type-safe ở cả backend và frontend (nếu dùng TypeScript)

---

## Migration Notes

### Đối với dữ liệu hiện có:
- Hibernate (ddl-auto: update) sẽ tự động drop các cột `brand`, `battery_capacity_kwh`, `battery_type`
- Dữ liệu trong cột `model` được giữ nguyên
- Các thông tin battery/brand sẽ được lấy từ enum khi query

### Backup recommendation:
```sql
-- Backup trước khi deploy (optional)
CREATE TABLE vehicles_backup AS SELECT * FROM vehicles;
```

---

**Last Updated:** October 25, 2025  
**Version:** 2.0 (Post-Refactoring)
