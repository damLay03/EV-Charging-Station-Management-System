package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.VehicleUpdateRequest;
import com.swp.evchargingstation.dto.response.VehicleResponse;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.entity.Vehicle;
import com.swp.evchargingstation.enums.Role;
import com.swp.evchargingstation.enums.VehicleModel;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.VehicleMapper;
import com.swp.evchargingstation.repository.DriverRepository;
import com.swp.evchargingstation.repository.UserRepository;
import com.swp.evchargingstation.repository.VehicleRepository;
import com.swp.evchargingstation.repository.ChargingSessionRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VehicleService {
    VehicleRepository vehicleRepository;
    DriverRepository driverRepository;
    UserRepository userRepository;
    ChargingSessionRepository chargingSessionRepository;
    VehicleMapper vehicleMapper;
    com.swp.evchargingstation.repository.AdminRepository adminRepository;
    CloudinaryService cloudinaryService;
    EmailService emailService;

    // NOTE: Driver tạo xe mới và upload 4 ảnh trong 1 request
    @PreAuthorize("hasRole('DRIVER')")
    @Transactional
    public VehicleResponse createVehicleWithDocument(String modelStr, String licensePlate,
                                                      org.springframework.web.multipart.MultipartFile documentFrontImage,
                                                      org.springframework.web.multipart.MultipartFile documentBackImage,
                                                      org.springframework.web.multipart.MultipartFile frontImage,
                                                      org.springframework.web.multipart.MultipartFile sideLeftImage,
                                                      org.springframework.web.multipart.MultipartFile sideRightImage,
                                                      org.springframework.web.multipart.MultipartFile rearImage) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Driver driver = driverRepository.findById(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Validate biển số xe không trùng
        if (vehicleRepository.existsByLicensePlate(licensePlate)) {
            throw new AppException(ErrorCode.LICENSE_PLATE_EXISTED);
        }

        // Parse VehicleModel from string
        VehicleModel model;
        try {
            model = VehicleModel.valueOf(modelStr);
        } catch (IllegalArgumentException e) {
            log.error("Invalid vehicle model: {}", modelStr);
            throw new AppException(ErrorCode.INVALID_VEHICLE_MODEL_FOR_BRAND);
        }

        log.info("Driver '{}' creating vehicle with license plate '{}', model '{}' (uploading 6 images)",
                user.getUserId(), licensePlate, model);

        // Step 1: Upload 6 ảnh lên Cloudinary
        log.info("Uploading 6 vehicle images for vehicle {}", licensePlate);

        String documentFrontImageUrl = cloudinaryService.uploadVehicleDocument(documentFrontImage);
        log.info("1/6 - Document front image uploaded: {}", documentFrontImageUrl);

        String documentBackImageUrl = cloudinaryService.uploadVehicleDocument(documentBackImage);
        log.info("2/6 - Document back image uploaded: {}", documentBackImageUrl);

        String frontImageUrl = cloudinaryService.uploadVehicleDocument(frontImage);
        log.info("3/6 - Front image uploaded: {}", frontImageUrl);

        String sideLeftImageUrl = cloudinaryService.uploadVehicleDocument(sideLeftImage);
        log.info("4/6 - Side left image uploaded: {}", sideLeftImageUrl);

        String sideRightImageUrl = cloudinaryService.uploadVehicleDocument(sideRightImage);
        log.info("5/6 - Side right image uploaded: {}", sideRightImageUrl);

        String rearImageUrl = cloudinaryService.uploadVehicleDocument(rearImage);
        log.info("6/6 - Rear image uploaded: {}", rearImageUrl);

        // Step 2: Tạo vehicle với 6 URL ảnh
        int randomSoc = 20 + (int) (Math.random() * 61); // Random từ 20 đến 80

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(licensePlate)
                .model(model)
                .owner(driver)
                .currentSocPercent(randomSoc)
                // Lưu các giá trị từ enum vào database
                .batteryCapacityKwhValue(model.getBatteryCapacityKwh())
                .batteryTypeValue(model.getBatteryType())
                .brandValue(model.getBrand())
                .maxChargingPowerValue(model.getMaxChargingPower())
                .maxChargingPowerKwValue(model.getMaxChargingPowerKw())
                // Approval fields - 6 ảnh
                .documentFrontImageUrl(documentFrontImageUrl)
                .documentBackImageUrl(documentBackImageUrl)
                .frontImageUrl(frontImageUrl)
                .sideLeftImageUrl(sideLeftImageUrl)
                .sideRightImageUrl(sideRightImageUrl)
                .rearImageUrl(rearImageUrl)
                .approvalStatus(com.swp.evchargingstation.enums.VehicleRegistrationStatus.PENDING)
                .submittedAt(java.time.LocalDateTime.now())
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle '{}' submitted for approval, brand '{}', SOC {}%, status: PENDING, 6 images uploaded",
                saved.getVehicleId(), saved.getBrand(), saved.getCurrentSocPercent());

        return vehicleMapper.toVehicleResponse(saved);
    }


    // NOTE: Driver lấy danh sách xe đã được phê duyệt (APPROVED) - dùng để chọn xe khi sạc
    @PreAuthorize("hasRole('DRIVER')")
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyApprovedVehicles() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("Driver '{}' fetching their APPROVED vehicles", user.getUserId());

        List<Vehicle> vehicles = vehicleRepository.findByOwner_UserIdAndApprovalStatus(
                user.getUserId(),
                com.swp.evchargingstation.enums.VehicleRegistrationStatus.APPROVED
        );
        return vehicles.stream()
                .map(vehicleMapper::toVehicleResponse)
                .toList();
    }

    // NOTE: Driver lấy TẤT CẢ yêu cầu đăng ký xe (PENDING, APPROVED, REJECTED)
    @PreAuthorize("hasRole('DRIVER')")
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyAllVehicleRequests() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("Driver '{}' fetching ALL their vehicle registration requests", user.getUserId());

        List<Vehicle> vehicles = vehicleRepository.findByOwner_UserId(user.getUserId());
        return vehicles.stream()
                .map(vehicleMapper::toVehicleResponse)
                .toList();
    }

    // NOTE: Driver lấy chi tiết một xe của chính mình
    @PreAuthorize("hasRole('DRIVER')")
    @Transactional(readOnly = true)
    public VehicleResponse getMyVehicle(String vehicleId) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Kiểm tra xe có thuộc về driver này không
        if (!vehicle.getOwner().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.VEHICLE_NOT_BELONG_TO_DRIVER);
        }

        return vehicleMapper.toVehicleResponse(vehicle);
    }

    // NOTE: Driver cập nhật thông tin xe của chính mình
    @PreAuthorize("hasRole('DRIVER')")
    @Transactional
    public VehicleResponse updateMyVehicle(String vehicleId, VehicleUpdateRequest request) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Kiểm tra xe có thuộc về driver này không
        if (!vehicle.getOwner().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.VEHICLE_NOT_BELONG_TO_DRIVER);
        }

        log.info("Driver '{}' updating vehicle '{}'", user.getUserId(), vehicleId);

        // Update license plate (nếu có)
        if (request.getLicensePlate() != null) {
            // Kiểm tra biển số mới không trùng với xe khác
            if (!vehicle.getLicensePlate().equals(request.getLicensePlate()) &&
                vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
                throw new AppException(ErrorCode.LICENSE_PLATE_EXISTED);
            }
            vehicle.setLicensePlate(request.getLicensePlate());
        }

        // Update model (nếu có) - brand sẽ tự động được xác định từ model
        if (request.getModel() != null) {
            VehicleModel newModel = request.getModel();
            vehicle.setModel(newModel);
            // Cập nhật các giá trị vào database
            vehicle.setBatteryCapacityKwhValue(newModel.getBatteryCapacityKwh());
            vehicle.setBatteryTypeValue(newModel.getBatteryType());
            vehicle.setBrandValue(newModel.getBrand());
            vehicle.setMaxChargingPowerValue(newModel.getMaxChargingPower());
            vehicle.setMaxChargingPowerKwValue(newModel.getMaxChargingPowerKw());
            log.info("Vehicle '{}' model updated to '{}', brand auto-updated to '{}'",
                    vehicleId, request.getModel(), vehicle.getBrand());
        }

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle '{}' updated successfully", vehicleId);

        return vehicleMapper.toVehicleResponse(saved);
    }

    // NOTE: Driver xóa xe của chính mình
    @PreAuthorize("hasRole('DRIVER')")
    @Transactional
    public void deleteMyVehicle(String vehicleId) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Kiểm tra xe có thuộc về driver này không
        if (!vehicle.getOwner().getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.VEHICLE_NOT_BELONG_TO_DRIVER);
        }

        log.info("Driver '{}' deleting vehicle '{}'", user.getUserId(), vehicleId);
        vehicleRepository.delete(vehicle);
        log.info("Vehicle '{}' deleted successfully", vehicleId);
    }

    // NOTE: ADMIN lấy danh sách xe của một driver
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByDriver(String driverId) {
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.DRIVER) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        log.info("Admin fetching vehicles of driver '{}'", driverId);

        List<Vehicle> vehicles = vehicleRepository.findByOwner_UserId(driverId);
        return vehicles.stream()
                .map(vehicleMapper::toVehicleResponse)
                .toList();
    }

    // NOTE: Lấy danh sách tất cả các hãng xe
    public List<com.swp.evchargingstation.dto.response.VehicleBrandResponse> getAllBrands() {
        log.info("Fetching all vehicle brands");
        return java.util.Arrays.stream(com.swp.evchargingstation.enums.VehicleBrand.values())
                .map(brand -> com.swp.evchargingstation.dto.response.VehicleBrandResponse.builder()
                        .brand(brand)
                        .displayName(brand.getDisplayName())
                        .country(brand.getCountry())
                        .build())
                .toList();
    }

    /**
     * Normalize license plate format
     * Converts: 51A12345 -> 51A-12345
     * Supports formats: 51A12345, 30A11111, 80B99999, etc.
     */
    private String normalizeLicensePlate(String licensePlate) {
        if (licensePlate == null || licensePlate.isEmpty()) {
            return licensePlate;
        }

        // Remove all dashes first
        String clean = licensePlate.replaceAll("-", "").trim().toUpperCase();

        // Pattern: 2-3 digits + 1 letter + 4-5 digits
        // Examples: 51A12345, 30A11111, 80B99999
        if (clean.matches("^\\d{2,3}[A-Z]\\d{4,5}$")) {
            // Find the position of the letter
            int letterIndex = -1;
            for (int i = 0; i < clean.length(); i++) {
                if (Character.isLetter(clean.charAt(i))) {
                    letterIndex = i;
                    break;
                }
            }

            if (letterIndex > 0) {
                // Insert dash after the letter: 51A -> 51A-
                return clean.substring(0, letterIndex + 1) + "-" + clean.substring(letterIndex + 1);
            }
        }

        // Return as-is if doesn't match pattern (might already have dash)
        return licensePlate.trim().toUpperCase();
    }

    // NOTE: Lấy danh sách models theo brand
    public List<com.swp.evchargingstation.dto.response.VehicleModelResponse> getModelsByBrand(com.swp.evchargingstation.enums.VehicleBrand brand) {
        log.info("Fetching models for brand '{}'", brand);
        return java.util.Arrays.stream(VehicleModel.getModelsByBrand(brand))
                .map(model -> com.swp.evchargingstation.dto.response.VehicleModelResponse.builder()
                        .model(model)
                        .modelName(model.getModelName())
                        .brand(model.getBrand())
                        .batteryCapacityKwh(model.getBatteryCapacityKwh())
                        .batteryType(model.getBatteryType())
                        .maxChargingPower(model.getMaxChargingPower())
                        .maxChargingPowerKw(model.getMaxChargingPowerKw())
                        .build())
                .toList();
    }

    /**
     * NOTE: STAFF - Lookup vehicle by license plate
     * Returns all necessary information to start a charging session
     */
    @PreAuthorize("hasRole('STAFF')")
    @Transactional(readOnly = true)
    public com.swp.evchargingstation.dto.response.VehicleLookupResponse lookupVehicleByLicensePlate(String licensePlate) {
        log.info("Staff looking up vehicle with license plate: {}", licensePlate);

        // Normalize license plate: 51A12345 -> 51A-12345
        String normalizedPlate = normalizeLicensePlate(licensePlate);
        log.info("Normalized license plate: {} -> {}", licensePlate, normalizedPlate);

        Vehicle vehicle = vehicleRepository.findByLicensePlate(normalizedPlate)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Get owner (driver) information
        Driver owner = vehicle.getOwner();
        if (owner == null) {
            throw new AppException(ErrorCode.DRIVER_NOT_FOUND);
        }

        User ownerUser = owner.getUser();
        if (ownerUser == null) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        // Check if vehicle has an active charging session
        boolean hasActiveSession = false;
        String activeSessionId = null;

        Optional<com.swp.evchargingstation.entity.ChargingSession> activeSession =
            chargingSessionRepository.findByVehicleIdAndStatus(
                vehicle.getVehicleId(),
                com.swp.evchargingstation.enums.ChargingSessionStatus.IN_PROGRESS
            );

        if (activeSession.isPresent()) {
            hasActiveSession = true;
            activeSessionId = activeSession.get().getSessionId();
        }

        log.info("Vehicle found: {} - Owner: {} ({})", vehicle.getVehicleId(),
                ownerUser.getFullName(), ownerUser.getEmail());

        return com.swp.evchargingstation.dto.response.VehicleLookupResponse.builder()
                // Vehicle information
                .vehicleId(vehicle.getVehicleId())
                .licensePlate(vehicle.getLicensePlate())
                .model(vehicle.getModel())
                .modelName(vehicle.getModel() != null ? vehicle.getModel().getModelName() : null)
                .brand(vehicle.getBrand())
                .brandDisplayName(vehicle.getBrand() != null ? vehicle.getBrand().getDisplayName() : null)
                .currentSocPercent(vehicle.getCurrentSocPercent())
                .batteryCapacityKwh(vehicle.getBatteryCapacityKwh())
                .batteryType(vehicle.getBatteryType())
                .maxChargingPower(vehicle.getMaxChargingPower())
                .maxChargingPowerKw(vehicle.getMaxChargingPowerKw())
                // Owner information
                .ownerId(ownerUser.getUserId())
                .ownerName(ownerUser.getFullName())
                .ownerEmail(ownerUser.getEmail())
                .ownerPhone(ownerUser.getPhone())
                // Session status
                .hasActiveSession(hasActiveSession)
                .activeSessionId(activeSessionId)
                .build();
    }

    public List<com.swp.evchargingstation.dto.response.VehicleModelResponse> getAllModels() {
        log.info("Fetching all vehicle models");
        return java.util.Arrays.stream(VehicleModel.values())
                .map(model -> com.swp.evchargingstation.dto.response.VehicleModelResponse.builder()
                        .model(model)
                        .modelName(model.getModelName())
                        .brand(model.getBrand())
                        .batteryCapacityKwh(model.getBatteryCapacityKwh())
                        .batteryType(model.getBatteryType())

                        .maxChargingPower(model.getMaxChargingPower())
                        .maxChargingPowerKw(model.getMaxChargingPowerKw())
                        .build())
                .toList();
    }
    // ==================== VEHICLE APPROVAL METHODS ====================

    // NOTE: ADMIN - Get all pending vehicle registrations
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<VehicleResponse> getPendingVehicles() {
        log.info("Admin fetching all pending vehicle registrations");
        List<Vehicle> vehicles = vehicleRepository.findByApprovalStatus(
                com.swp.evchargingstation.enums.VehicleRegistrationStatus.PENDING);
        return vehicles.stream()
                .map(vehicleMapper::toVehicleResponse)
                .toList();
    }

    // NOTE: ADMIN - Get all vehicles (with any status)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehiclesWithStatus() {
        log.info("Admin fetching all vehicles");
        List<Vehicle> vehicles = vehicleRepository.findAll();
        return vehicles.stream()
                .map(vehicleMapper::toVehicleResponse)
                .toList();
    }

    // NOTE: ADMIN - Approve vehicle registration
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public VehicleResponse approveVehicle(String vehicleId) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        com.swp.evchargingstation.entity.Admin admin = adminRepository.findById(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Check if already processed
        if (vehicle.getApprovalStatus() != com.swp.evchargingstation.enums.VehicleRegistrationStatus.PENDING) {
            throw new AppException(ErrorCode.VEHICLE_ALREADY_PROCESSED);
        }

        vehicle.setApprovalStatus(com.swp.evchargingstation.enums.VehicleRegistrationStatus.APPROVED);
        vehicle.setApprovedAt(java.time.LocalDateTime.now());
        vehicle.setApprovedBy(admin);
        vehicle.setRejectionReason(null);

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Admin '{}' approved vehicle '{}' with license plate '{}'",
                admin.getUserId(), vehicleId, vehicle.getLicensePlate());

        // Send email notification to driver
        try {
            emailService.sendVehicleApprovedEmail(vehicle.getOwner().getUser(), vehicle);
        } catch (Exception e) {
            log.error("Failed to send approval email for vehicle {}: {}", vehicleId, e.getMessage());
            // Don't fail the approval if email fails
        }

        return vehicleMapper.toVehicleResponse(saved);
    }

    // NOTE: ADMIN - Reject vehicle registration
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public VehicleResponse rejectVehicle(String vehicleId, String rejectionReason) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        com.swp.evchargingstation.entity.Admin admin = adminRepository.findById(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADMIN_NOT_FOUND));

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND));

        // Check if already processed
        if (vehicle.getApprovalStatus() != com.swp.evchargingstation.enums.VehicleRegistrationStatus.PENDING) {
            throw new AppException(ErrorCode.VEHICLE_ALREADY_PROCESSED);
        }

        vehicle.setApprovalStatus(com.swp.evchargingstation.enums.VehicleRegistrationStatus.REJECTED);
        vehicle.setApprovedAt(java.time.LocalDateTime.now());
        vehicle.setApprovedBy(admin);
        vehicle.setRejectionReason(rejectionReason);

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Admin '{}' rejected vehicle '{}' with license plate '{}'. Reason: {}",
                admin.getUserId(), vehicleId, vehicle.getLicensePlate(), rejectionReason);

        // Send email notification to driver
        try {
            emailService.sendVehicleRejectedEmail(vehicle.getOwner().getUser(), vehicle, rejectionReason);
        } catch (Exception e) {
            log.error("Failed to send rejection email for vehicle {}: {}", vehicleId, e.getMessage());
            // Don't fail the rejection if email fails
        }

        return vehicleMapper.toVehicleResponse(saved);
    }
}
