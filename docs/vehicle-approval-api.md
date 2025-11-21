# Vehicle Registration & Approval API

API để quản lý việc đăng ký và phê duyệt xe của driver.

## Flow hoạt động

1. **Driver nộp đơn đăng ký xe** (1 bước duy nhất):
   - Driver điền form với thông tin xe + upload **6 ảnh bắt buộc**
   - Gọi API `POST /api/vehicles` (multipart/form-data) với:
     - `model`: Mẫu xe (VD: TESLA_MODEL_3)
     - `licensePlate`: Biển số xe (VD: 30A-12345)
     - **6 file ảnh bắt buộc:**
       1. `documentFrontImage`: Ảnh mặt trước cà vẹt (giấy đăng ký xe)
       2. `documentBackImage`: Ảnh mặt sau cà vẹt
       3. `frontImage`: Ảnh đầu xe
       4. `sideLeftImage`: Ảnh thân xe bên trái
       5. `sideRightImage`: Ảnh thân xe bên phải
       6. `rearImage`: Ảnh đuôi xe
   - Hệ thống tự động upload ảnh lên Cloudinary
   - Status mặc định: **PENDING** (chờ admin phê duyệt)

2. **Admin xét duyệt**:
   - Xem danh sách xe chờ duyệt với đầy đủ 6 ảnh
   - **Approve** → Xe được phê duyệt, driver có thể sử dụng để sạc
   - **Reject** → Nhập lý do từ chối, gửi email cho driver

3. **Driver nhận kết quả**:
   - Nhận email thông báo khi xe được approve/reject
   - Xem trạng thái xe trong app
   - Nếu bị reject: có thể xóa và nộp lại đơn mới

---

## API Endpoints

### 1. Driver - Đăng ký xe mới (với 6 ảnh)

**Endpoint:** `POST /api/vehicles`

**Authorization:** Bearer Token (Role: DRIVER)

**Content-Type:** `multipart/form-data`

**Request Parameters:**
- `model` (String, required): Mẫu xe. VD: `TESLA_MODEL_3`, `VINFAST_VF8`, `BYD_ATTO_3`
- `licensePlate` (String, required): Biển số xe. VD: `30A-12345`
- `documentFrontImage` (File, required): Ảnh mặt trước cà vẹt (jpg, jpeg, png). Max: 5MB
- `documentBackImage` (File, required): Ảnh mặt sau cà vẹt (jpg, jpeg, png). Max: 5MB
- `frontImage` (File, required): Ảnh đầu xe (jpg, jpeg, png). Max: 5MB
- `sideLeftImage` (File, required): Ảnh thân xe bên trái (jpg, jpeg, png). Max: 5MB
- `sideRightImage` (File, required): Ảnh thân xe bên phải (jpg, jpeg, png). Max: 5MB
- `rearImage` (File, required): Ảnh đuôi xe (jpg, jpeg, png). Max: 5MB

**Example using cURL:**
```bash
curl -X POST "http://localhost:8080/evchargingstation/api/vehicles" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "model=TESLA_MODEL_3" \
  -F "licensePlate=30A-12345" \
  -F "documentFrontImage=@/path/to/document-front.jpg" \
  -F "documentBackImage=@/path/to/document-back.jpg" \
  -F "frontImage=@/path/to/front.jpg" \
  -F "sideLeftImage=@/path/to/side-left.jpg" \
  -F "sideRightImage=@/path/to/side-right.jpg" \
  -F "rearImage=@/path/to/rear.jpg"
```

**Example using JavaScript (Fetch API):**
```javascript
const formData = new FormData();
formData.append('model', 'TESLA_MODEL_3');
formData.append('licensePlate', '30A-12345');
formData.append('documentFrontImage', documentFrontInput.files[0]);
formData.append('documentBackImage', documentBackInput.files[0]);
formData.append('frontImage', frontImageInput.files[0]);
formData.append('sideLeftImage', sideLeftInput.files[0]);
formData.append('sideRightImage', sideRightInput.files[0]);
formData.append('rearImage', rearImageInput.files[0]);

fetch('/evchargingstation/api/vehicles', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`
  },
  body: formData
})
.then(response => response.json())
.then(data => {
  if (data.code === 1000) {
    alert('✅ Đã gửi yêu cầu đăng ký xe! Vui lòng đợi admin phê duyệt.');
  }
});
```

**Example React Component:**
```jsx
function AddVehicleForm() {
  const [model, setModel] = useState('TESLA_MODEL_3');
  const [licensePlate, setLicensePlate] = useState('');
  const [images, setImages] = useState({
    documentFront: null,
    documentBack: null,
    front: null,
    sideLeft: null,
    sideRight: null,
    rear: null
  });
  const [loading, setLoading] = useState(false);

  const handleImageChange = (imageType, file) => {
    setImages(prev => ({ ...prev, [imageType]: file }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Validate all 6 images
    if (!Object.values(images).every(img => img !== null)) {
      alert('❌ Vui lòng upload đầy đủ 6 ảnh!');
      return;
    }
    
    setLoading(true);

    try {
      const formData = new FormData();
      formData.append('model', model);
      formData.append('licensePlate', licensePlate);
      formData.append('documentFrontImage', images.documentFront);
      formData.append('documentBackImage', images.documentBack);
      formData.append('frontImage', images.front);
      formData.append('sideLeftImage', images.sideLeft);
      formData.append('sideRightImage', images.sideRight);
      formData.append('rearImage', images.rear);

      const response = await fetch('/evchargingstation/api/vehicles', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData
      });

      const data = await response.json();
      
      if (data.code === 1000) {
        alert('✅ Đã gửi yêu cầu đăng ký xe thành công!');
        // Reset form hoặc redirect
      } else {
        alert('❌ Có lỗi: ' + data.message);
      }
    } catch (error) {
      alert('❌ Có lỗi xảy ra: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const ImageUpload = ({ label, imageType, required = true }) => (
    <div className="image-upload">
      <label>{label}:</label>
      <input 
        type="file" 
        accept="image/*"
        onChange={(e) => handleImageChange(imageType, e.target.files[0])}
        required={required}
      />
      {images[imageType] && (
        <div className="preview">
          <p>✓ {images[imageType].name}</p>
          <img 
            src={URL.createObjectURL(images[imageType])} 
            alt="Preview" 
            style={{maxWidth: '200px', maxHeight: '150px'}}
          />
        </div>
      )}
    </div>
  );

  return (
    <form onSubmit={handleSubmit}>
      <h2>Đăng ký xe mới</h2>
      
      <label>Model xe:</label>
      <select value={model} onChange={(e) => setModel(e.target.value)} required>
        <option value="TESLA_MODEL_3">Tesla Model 3</option>
        <option value="VINFAST_VF8">VinFast VF8</option>
        <option value="BYD_ATTO_3">BYD Atto 3</option>
      </select>

      <label>Biển số xe:</label>
      <input 
        type="text" 
        value={licensePlate}
        onChange={(e) => setLicensePlate(e.target.value)}
        placeholder="VD: 30A-12345"
        required
      />

      <h3>Upload 6 ảnh xe (bắt buộc):</h3>
      <ImageUpload label="1. Ảnh mặt trước cà vẹt" imageType="documentFront" />
      <ImageUpload label="2. Ảnh mặt sau cà vẹt" imageType="documentBack" />
      <ImageUpload label="3. Ảnh đầu xe" imageType="front" />
      <ImageUpload label="4. Ảnh thân xe - bên trái" imageType="sideLeft" />
      <ImageUpload label="5. Ảnh thân xe - bên phải" imageType="sideRight" />
      <ImageUpload label="6. Ảnh đuôi xe" imageType="rear" />

      <button type="submit" disabled={loading}>
        {loading ? 'Đang upload...' : 'Gửi yêu cầu đăng ký'}
      </button>
    </form>
  );
}
```

**Success Response:**
```json
{
  "code": 1000,
  "message": "Vehicle registration submitted successfully with 6 images. Please wait for admin approval.",
  "result": {
    "vehicleId": "uuid-here",
    "licensePlate": "30A-12345",
    "vin": "WVWZZZ1JZYW123456",
    "model": "TESLA_MODEL_3",
    "brand": "TESLA",
    "batteryCapacityKwh": 60.0,
    "batteryType": "LFP",
    "maxChargingPower": "DC_FAST",
    "maxChargingPowerKw": 170.0,
    "imageUrl": "https://res.cloudinary.com/.../tesla-model-3.png",
    "ownerId": "driver-uuid",
    "ownerName": "Nguyễn Văn A",
    "ownerEmail": "driver@example.com",
    "ownerPhone": "0912345678",
    "currentSocPercent": 65,
    "documentFrontImageUrl": "https://res.cloudinary.com/.../vehicle-documents/doc-front123.jpg",
    "documentBackImageUrl": "https://res.cloudinary.com/.../vehicle-documents/doc-back123.jpg",
    "frontImageUrl": "https://res.cloudinary.com/.../vehicle-documents/front123.jpg",
    "sideLeftImageUrl": "https://res.cloudinary.com/.../vehicle-documents/side-left123.jpg",
    "sideRightImageUrl": "https://res.cloudinary.com/.../vehicle-documents/side-right123.jpg",
    "rearImageUrl": "https://res.cloudinary.com/.../vehicle-documents/rear123.jpg",
    "approvalStatus": "PENDING",
    "rejectionReason": null,
    "submittedAt": "2025-11-21T14:30:00",
    "approvedAt": null,
    "approvedByAdminId": null,
    "approvedByAdminName": null
  }
}
```

**Error Responses:**

- **400 Bad Request** - Thiếu ảnh (phải đủ 6 ảnh)
```json
{
  "code": 23005,
  "message": "Missing Required Images. All 6 images are required."
}
```

- **400 Bad Request** - Biển số đã tồn tại
```json
{
  "code": 5002,
  "message": "License Plate Already Exists"
}
```

- **400 Bad Request** - VIN đã tồn tại
```json
{
  "code": 5005,
  "message": "VIN Already Exists"
}
```

- **400 Bad Request** - File không hợp lệ
```json
{
  "code": 23001,
  "message": "Invalid File"
}
```

- **400 Bad Request** - Loại file không hợp lệ
```json
{
  "code": 23002,
  "message": "Invalid File Type. Only Images Are Allowed"
}
```

- **400 Bad Request** - File quá lớn
```json
{
  "code": 23003,
  "message": "File Size Exceeds 5MB Limit"
}
```

- **400 Bad Request** - Model không hợp lệ
```json
{
  "code": 5004,
  "message": "Vehicle Model Does Not Match Selected Brand"
}
```

- **500 Internal Server Error** - Upload thất bại
```json
{
  "code": 23004,
  "message": "Failed To Upload File"
}
```

---

### 2. Driver - Xem danh sách xe APPROVED (để sử dụng sạc)

**Endpoint:** `GET /api/vehicles`

**Authorization:** Bearer Token (Role: DRIVER)

**Description:** Chỉ trả về các xe đã được phê duyệt (APPROVED) để driver chọn khi booking/charging

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "vehicleId": "uuid-1",
      "licensePlate": "30A-12345",
      "vin": "WVWZZZ1JZYW123456",
      "model": "TESLA_MODEL_3",
      "brand": "TESLA",
      "currentSocPercent": 65,
      "documentFrontImageUrl": "https://res.cloudinary.com/.../doc-front1.jpg",
      "documentBackImageUrl": "https://res.cloudinary.com/.../doc-back1.jpg",
      "frontImageUrl": "https://res.cloudinary.com/.../front1.jpg",
      "sideLeftImageUrl": "https://res.cloudinary.com/.../side-left1.jpg",
      "sideRightImageUrl": "https://res.cloudinary.com/.../side-right1.jpg",
      "rearImageUrl": "https://res.cloudinary.com/.../rear1.jpg",
      "approvalStatus": "APPROVED",
      "rejectionReason": null,
      "submittedAt": "2025-11-15T10:30:00",
      "approvedAt": "2025-11-16T14:20:00",
      "approvedByAdminName": "Nguyễn Văn A"
    }
  ]
}
```

---

### 3. Driver - Xem TẤT CẢ yêu cầu đăng ký (PENDING, APPROVED, REJECTED)

**Endpoint:** `GET /api/vehicles/my-requests`

**Authorization:** Bearer Token (Role: DRIVER)

**Description:** Trả về tất cả yêu cầu đăng ký xe của driver, bao gồm cả xe chờ duyệt, đã duyệt, và bị từ chối

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "vehicleId": "uuid-1",
      "licensePlate": "30A-12345",
      "vin": "WVWZZZ1JZYW123456",
      "model": "TESLA_MODEL_3",
      "brand": "TESLA",
      "documentFrontImageUrl": "https://res.cloudinary.com/.../doc-front1.jpg",
      "documentBackImageUrl": "https://res.cloudinary.com/.../doc-back1.jpg",
      "frontImageUrl": "https://res.cloudinary.com/.../front1.jpg",
      "sideLeftImageUrl": "https://res.cloudinary.com/.../side-left1.jpg",
      "sideRightImageUrl": "https://res.cloudinary.com/.../side-right1.jpg",
      "rearImageUrl": "https://res.cloudinary.com/.../rear1.jpg",
      "approvalStatus": "APPROVED",
      "rejectionReason": null,
      "submittedAt": "2025-11-15T10:30:00",
      "approvedAt": "2025-11-16T14:20:00",
      "approvedByAdminName": "Nguyễn Văn A"
    },
    {
      "vehicleId": "uuid-2",
      "licensePlate": "51F-67890",
      "vin": "5YJSA1E26HF123456",
      "model": "VINFAST_VF8",
      "brand": "VINFAST",
      "documentFrontImageUrl": "https://res.cloudinary.com/.../doc-front2.jpg",
      "documentBackImageUrl": "https://res.cloudinary.com/.../doc-back2.jpg",
      "frontImageUrl": "https://res.cloudinary.com/.../front2.jpg",
      "sideLeftImageUrl": "https://res.cloudinary.com/.../side-left2.jpg",
      "sideRightImageUrl": "https://res.cloudinary.com/.../side-right2.jpg",
      "rearImageUrl": "https://res.cloudinary.com/.../rear2.jpg",
      "approvalStatus": "PENDING",
      "rejectionReason": null,
      "submittedAt": "2025-11-20T10:30:00",
      "approvedAt": null,
      "approvedByAdminName": null
    },
    {
      "vehicleId": "uuid-3",
      "licensePlate": "80B-11111",
      "vin": "LYVBR33F3MB123456",
      "model": "BYD_ATTO_3",
      "brand": "BYD",
      "documentFrontImageUrl": "https://res.cloudinary.com/.../doc-front3.jpg",
      "documentBackImageUrl": "https://res.cloudinary.com/.../doc-back3.jpg",
      "frontImageUrl": "https://res.cloudinary.com/.../front3.jpg",
      "sideLeftImageUrl": "https://res.cloudinary.com/.../side-left3.jpg",
      "sideRightImageUrl": "https://res.cloudinary.com/.../side-right3.jpg",
      "rearImageUrl": "https://res.cloudinary.com/.../rear3.jpg",
      "approvalStatus": "REJECTED",
      "rejectionReason": "Giấy tờ xe không rõ ràng, vui lòng chụp lại 6 ảnh với ánh sáng tốt hơn",
      "submittedAt": "2025-11-18T09:00:00",
      "approvedAt": "2025-11-18T15:00:00",
      "approvedByAdminName": "Trần Thị B"
    }
  ]
}
```

---

### 4. Admin - Xem danh sách xe chờ phê duyệt

**Endpoint:** `GET /api/vehicles/pending`

**Authorization:** Bearer Token (Role: ADMIN)

**Description:** Admin xem tất cả xe đang chờ phê duyệt với đầy đủ 6 ảnh và thông tin driver

**Response:**
```json
{
  "code": 1000,
  "result": [
    {
      "vehicleId": "uuid-here",
      "licensePlate": "51F-67890",
      "vin": "5YJSA1E26HF123456",
      "model": "VINFAST_VF8",
      "brand": "VINFAST",
      "ownerId": "driver-uuid",
      "ownerName": "Nguyễn Văn B",
      "ownerEmail": "driverb@example.com",
      "ownerPhone": "0987654321",
      "documentFrontImageUrl": "https://res.cloudinary.com/.../doc-front2.jpg",
      "documentBackImageUrl": "https://res.cloudinary.com/.../doc-back2.jpg",
      "frontImageUrl": "https://res.cloudinary.com/.../front2.jpg",
      "sideLeftImageUrl": "https://res.cloudinary.com/.../side-left2.jpg",
      "sideRightImageUrl": "https://res.cloudinary.com/.../side-right2.jpg",
      "rearImageUrl": "https://res.cloudinary.com/.../rear2.jpg",
      "approvalStatus": "PENDING",
      "rejectionReason": null,
      "submittedAt": "2025-11-20T10:30:00",
      "approvedAt": null
    }
  ]
}
```

---

### 5. Admin - Xem tất cả xe (bao gồm tất cả trạng thái)

**Endpoint:** `GET /api/vehicles/all-status`

**Authorization:** Bearer Token (Role: ADMIN)

**Description:** Admin xem tất cả xe trong hệ thống, bao gồm PENDING, APPROVED, REJECTED

**Response:** Tương tự endpoint trên, nhưng bao gồm tất cả xe với đầy đủ 6 ảnh

---

### 6. Admin - Phê duyệt xe

**Endpoint:** `PUT /api/vehicles/{vehicleId}/approve`

**Authorization:** Bearer Token (Role: ADMIN)

**Path Parameter:**
- `vehicleId`: ID của xe cần phê duyệt

**Response:**
```json
{
  "code": 1000,
  "message": "Vehicle approved successfully. Email notification sent to driver.",
  "result": {
    "vehicleId": "uuid-here",
    "licensePlate": "51F-67890",
    "vin": "5YJSA1E26HF123456",
    "model": "VINFAST_VF8",
    "brand": "VINFAST",
    "documentFrontImageUrl": "https://res.cloudinary.com/.../doc-front2.jpg",
    "documentBackImageUrl": "https://res.cloudinary.com/.../doc-back2.jpg",
    "frontImageUrl": "https://res.cloudinary.com/.../front2.jpg",
    "sideLeftImageUrl": "https://res.cloudinary.com/.../side-left2.jpg",
    "sideRightImageUrl": "https://res.cloudinary.com/.../side-right2.jpg",
    "rearImageUrl": "https://res.cloudinary.com/.../rear2.jpg",
    "approvalStatus": "APPROVED",
    "rejectionReason": null,
    "submittedAt": "2025-11-20T10:30:00",
    "approvedAt": "2025-11-20T14:45:00",
    "approvedByAdminName": "Nguyễn Văn A"
  }
}
```

**Error Responses:**

- **404 Not Found** - Xe không tồn tại
```json
{
  "code": 5001,
  "message": "Vehicle Not Found"
}
```

- **400 Bad Request** - Xe đã được xử lý rồi
```json
{
  "code": 22001,
  "message": "Vehicle Registration Already Processed"
}
```

---

### 7. Admin - Từ chối xe

**Endpoint:** `PUT /api/vehicles/{vehicleId}/reject`

**Authorization:** Bearer Token (Role: ADMIN)

**Path Parameter:**
- `vehicleId`: ID của xe cần từ chối

**Query Parameter:**
- `rejectionReason` (required): Lý do từ chối

**Example:**
```bash
curl -X PUT "http://localhost:8080/evchargingstation/api/vehicles/{vehicleId}/reject?rejectionReason=Giấy%20tờ%20không%20rõ%20ràng" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

**Response:**
```json
{
  "code": 1000,
  "message": "Vehicle rejected. Email notification sent to driver.",
  "result": {
    "vehicleId": "uuid-here",
    "licensePlate": "51F-67890",
    "vin": "5YJSA1E26HF123456",
    "model": "VINFAST_VF8",
    "brand": "VINFAST",
    "documentFrontImageUrl": "https://res.cloudinary.com/.../doc-front2.jpg",
    "documentBackImageUrl": "https://res.cloudinary.com/.../doc-back2.jpg",
    "frontImageUrl": "https://res.cloudinary.com/.../front2.jpg",
    "sideLeftImageUrl": "https://res.cloudinary.com/.../side-left2.jpg",
    "sideRightImageUrl": "https://res.cloudinary.com/.../side-right2.jpg",
    "rearImageUrl": "https://res.cloudinary.com/.../rear2.jpg",
    "approvalStatus": "REJECTED",
    "rejectionReason": "Giấy tờ xe không rõ ràng, vui lòng chụp lại 6 ảnh với ánh sáng tốt hơn",
    "submittedAt": "2025-11-20T10:30:00",
    "approvedAt": "2025-11-20T14:45:00",
    "approvedByAdminName": "Nguyễn Văn A"
  }
}
```

---

## Error Codes

| Code | Message | Description |
|------|---------|-------------|
| 1000 | Success | Thành công |
| 5001 | Vehicle Not Found | Xe không tồn tại |
| 5002 | License Plate Already Exists | Biển số xe đã tồn tại |
| 5004 | Vehicle Model Does Not Match Selected Brand | Model xe không khớp với brand |
| 5005 | VIN Already Exists | Số khung xe (VIN) đã tồn tại |
| 22001 | Vehicle Registration Already Processed | Xe đã được xử lý (approved/rejected) rồi |
| 22002 | Vehicle Not Approved For Charging | Xe chưa được phê duyệt để sạc |
| 23001 | Invalid File | File không hợp lệ |
| 23002 | Invalid File Type. Only Images Are Allowed | Chỉ chấp nhận file ảnh (jpg, jpeg, png) |
| 23003 | File Size Exceeds 5MB Limit | File quá lớn (tối đa 5MB mỗi ảnh) |
| 23004 | Failed To Upload File | Upload file lên Cloudinary thất bại |
| 23005 | Missing Required Images | Thiếu ảnh (phải đủ 6 ảnh) |

---

## Approval Status

- **PENDING**: Đang chờ admin xét duyệt (mặc định khi tạo xe mới)
- **APPROVED**: Đã được phê duyệt, xe có thể sử dụng để sạc
- **REJECTED**: Bị từ chối, driver có thể xóa và nộp lại đơn mới

---

## Frontend Flow Đề xuất

### Driver Side:

1. **Trang "My Approved Vehicles"** (GET /api/vehicles):
   - Hiển thị danh sách xe đã được approve
   - Thông tin: biển số, model, VIN, SOC%, 6 ảnh preview
   - Click để xem chi tiết và full-size ảnh

2. **Trang "My Vehicle Requests"** (GET /api/vehicles/my-requests):
   - Hiển thị TẤT CẢ yêu cầu đăng ký với badge trạng thái:
     - ✅ **APPROVED**: Xe đã duyệt, có checkmark xanh
     - ⏳ **PENDING**: Đang chờ duyệt, hiển thị "Waiting for approval"
     - ❌ **REJECTED**: Bị từ chối, hiển thị lý do và nút "Resubmit"
   - Click vào để xem 6 ảnh và thông tin chi tiết

3. **Form "Add New Vehicle"**:
   - Chọn model xe (Tesla Model 3, VinFast VF8, etc.)
   - Nhập biển số xe
   - **Upload 6 ảnh bắt buộc:**
     1. Ảnh mặt trước cà vẹt (giấy đăng ký xe)
     2. Ảnh mặt sau cà vẹt
     3. Ảnh đầu xe
     4. Ảnh thân xe bên trái
     5. Ảnh thân xe bên phải
     6. Ảnh đuôi xe
   - Preview tất cả ảnh trước khi upload
   - Hiển thị progress khi upload (VD: "Đang upload 3/6 ảnh...")
   - Submit và nhận thông báo "Đã gửi yêu cầu, chờ admin phê duyệt"

### Admin Side:

1. **Trang "Pending Vehicle Approvals"** (GET /api/vehicles/pending):
   - Danh sách xe chờ duyệt với thông tin:
     - Biển số, model, driver name, email, phone
     - Submitted date
     - Preview 6 ảnh (thumbnail)
   - Click vào xe để xem chi tiết:
     - View full size 6 ảnh (2 ảnh cà vẹt + 4 ảnh xe)
     - Xem thông tin biển số, model, driver
     - Nút "Approve" (màu xanh)
     - Nút "Reject" (màu đỏ, popup nhập lý do)

2. **Trang "All Vehicles"** (GET /api/vehicles/all-status):
   - Danh sách tất cả xe với filter theo status
   - Xem lịch sử phê duyệt (approved by, approved at)
   - Search theo biển số

3. **Admin Actions**:
   - **Approve**: Gửi email thông báo cho driver ✅
   - **Reject**: Nhập lý do, gửi email kèm lý do ❌

---

## Database Schema

Bảng `vehicles` có các columns liên quan đến approval:

```sql
-- Image URLs (6 ảnh)
document_front_image_url VARCHAR(500) NULL
document_back_image_url VARCHAR(500) NULL
front_image_url VARCHAR(500) NULL
side_left_image_url VARCHAR(500) NULL
side_right_image_url VARCHAR(500) NULL
rear_image_url VARCHAR(500) NULL

-- Approval workflow
approval_status VARCHAR(20) DEFAULT 'PENDING'  -- PENDING, APPROVED, REJECTED
rejection_reason VARCHAR(500) NULL
submitted_at DATETIME NULL
approved_at DATETIME NULL
approved_by VARCHAR(36) NULL  -- Admin user ID
```

---

## 📝 Important Notes

### 1. **6 Ảnh Bắt Buộc**
- Driver phải upload đủ **6 ảnh** khi đăng ký xe:
  1. **Cà vẹt mặt trước** - Giấy đăng ký xe (mặt có thông tin chính)
  2. **Cà vẹt mặt sau** - Mặt sau giấy đăng ký xe
  3. **Đầu xe** - Phía trước xe (nhìn rõ biển số)
  4. **Thân xe trái** - Bên hông trái của xe
  5. **Thân xe phải** - Bên hông phải của xe
  6. **Đuôi xe** - Phía sau xe
- Tất cả ảnh được lưu trên Cloudinary trong folder `vehicle-documents/`
- Max file size: 5MB mỗi ảnh
- Định dạng: JPG, JPEG, PNG

### 2. **Thông Tin Driver**
- Response bao gồm đầy đủ thông tin driver: `ownerName`, `ownerEmail`, `ownerPhone`
- Admin có thể liên hệ driver trực tiếp qua email/phone nếu cần
- Driver có thể xem thông tin xe của mình kèm status approval

### 3. **Xe Cũ (Backward Compatibility)**
- Xe cũ có thể không có đủ 6 ảnh nhưng vẫn hoạt động bình thường
- Chỉ xe mới đăng ký từ bây giờ mới bắt buộc 6 ảnh

### 4. **Email Notifications**
- ✅ **Đã implement**: Gửi email khi admin approve/reject
- Email khi APPROVED: Thông báo xe đã duyệt, có thể sạc ngay
- Email khi REJECTED: Kèm lý do từ chối chi tiết

### 5. **Driver Có Thể**
- Xem danh sách xe APPROVED để chọn khi sạc
- Xem TẤT CẢ yêu cầu đăng ký (PENDING, APPROVED, REJECTED)
- Xóa xe bị reject và nộp lại đơn mới
- Đăng ký không giới hạn số lượng xe

### 6. **Admin Có Thể**
- Xem danh sách xe chờ duyệt với đầy đủ 6 ảnh
- Xem tất cả xe trong hệ thống với filter theo status
- Phê duyệt hoặc từ chối xe
- Hệ thống tự động gửi email thông báo cho driver

### 7. **Security**
- Chỉ DRIVER có thể đăng ký xe
- Chỉ ADMIN có thể approve/reject
- Driver chỉ xem được xe của chính mình
- Admin xem được tất cả xe

---

## ✅ Implementation Checklist

### Backend Implementation:
- ✅ Vehicle Entity: 6 image URLs + approval workflow fields + driver info
- ✅ Controller: Nhận 6 file images (documentFront, documentBack, front, sideLeft, sideRight, rear)
- ✅ Service: Upload 6 ảnh lên Cloudinary với progress logging
- ✅ Repository: Methods cho approval workflow
- ✅ Email Service: Gửi email khi approve/reject
- ✅ Response DTOs: Trả về đầy đủ 6 URLs + thông tin driver (name, email, phone)
- ✅ Mapper: MapStruct tự động map thông tin driver từ owner.user

### API Endpoints:
1. ✅ `POST /api/vehicles` - Driver đăng ký xe với 6 ảnh
2. ✅ `GET /api/vehicles` - Driver xem xe APPROVED
3. ✅ `GET /api/vehicles/my-requests` - Driver xem TẤT CẢ yêu cầu
4. ✅ `GET /api/vehicles/pending` - Admin xem xe chờ duyệt
5. ✅ `GET /api/vehicles/all-status` - Admin xem tất cả xe
6. ✅ `PUT /api/vehicles/{id}/approve` - Admin phê duyệt + gửi email
7. ✅ `PUT /api/vehicles/{id}/reject` - Admin từ chối + gửi email với lý do

### Documentation:
- ✅ Complete API documentation với examples
- ✅ Frontend implementation guides (React, HTML/JS)
- ✅ Error codes table
- ✅ Database schema
- ✅ Flow diagram và user stories

---

**Lưu ý:** File này đã được cập nhật với flow mới nhất - bắt buộc **6 ảnh** thay vì 4 ảnh như trước đây.

