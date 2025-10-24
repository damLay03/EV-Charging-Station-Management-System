package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.VehicleCreationRequest;
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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VehicleService {
    VehicleRepository vehicleRepository;
    DriverRepository driverRepository;
    UserRepository userRepository;
    VehicleMapper vehicleMapper;

    // NOTE: Driver tạo xe mới cho chính mình
    @PreAuthorize("hasRole('DRIVER')")
    @Transactional
    public VehicleResponse createVehicle(VehicleCreationRequest request) {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Driver driver = driverRepository.findById(user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Validate biển số xe không trùng
        if (vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
            throw new AppException(ErrorCode.LICENSE_PLATE_EXISTED);
        }

        log.info("Driver '{}' creating vehicle with license plate '{}', model '{}'",
                user.getUserId(), request.getLicensePlate(), request.getModel());

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(request.getLicensePlate())
                .model(request.getModel())
                .owner(driver)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle '{}' created successfully with brand '{}' auto-detected from model",
                saved.getVehicleId(), saved.getBrand());

        return vehicleMapper.toVehicleResponse(saved);
    }

    // NOTE: Driver lấy danh sách xe của chính mình
    @PreAuthorize("hasRole('DRIVER')")
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles() {
        var context = SecurityContextHolder.getContext();
        String email = context.getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("Driver '{}' fetching their vehicles", user.getUserId());

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
            vehicle.setModel(request.getModel());
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
                        .build())
                .toList();
    }

    // NOTE: Lấy danh sách tất cả models
    public List<com.swp.evchargingstation.dto.response.VehicleModelResponse> getAllModels() {
        log.info("Fetching all vehicle models");
        return java.util.Arrays.stream(VehicleModel.values())
                .map(model -> com.swp.evchargingstation.dto.response.VehicleModelResponse.builder()
                        .model(model)
                        .modelName(model.getModelName())
                        .brand(model.getBrand())
                        .batteryCapacityKwh(model.getBatteryCapacityKwh())
                        .batteryType(model.getBatteryType())
                        .build())
                .toList();
    }
}
