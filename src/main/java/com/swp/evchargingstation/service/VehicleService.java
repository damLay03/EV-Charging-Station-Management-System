package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.VehicleCreationRequest;
import com.swp.evchargingstation.dto.request.VehicleUpdateRequest;
import com.swp.evchargingstation.dto.response.VehicleResponse;
import com.swp.evchargingstation.entity.Driver;
import com.swp.evchargingstation.entity.User;
import com.swp.evchargingstation.entity.Vehicle;
import com.swp.evchargingstation.enums.Role;
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

        log.info("Driver '{}' creating vehicle with license plate '{}'", user.getUserId(), request.getLicensePlate());

        Vehicle vehicle = Vehicle.builder()
                .licensePlate(request.getLicensePlate())
                .model(request.getModel())
                .batteryCapacityKwh(request.getBatteryCapacityKwh())
                .batteryType(request.getBatteryType())
                .owner(driver)
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle '{}' created successfully", saved.getVehicleId());

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

        // Update fields (chỉ update nếu không null)
        if (request.getLicensePlate() != null) {
            // Kiểm tra biển số mới không trùng với xe khác
            if (!vehicle.getLicensePlate().equals(request.getLicensePlate()) &&
                vehicleRepository.existsByLicensePlate(request.getLicensePlate())) {
                throw new AppException(ErrorCode.LICENSE_PLATE_EXISTED);
            }
            vehicle.setLicensePlate(request.getLicensePlate());
        }
        if (request.getModel() != null) {
            vehicle.setModel(request.getModel());
        }
        if (request.getBatteryCapacityKwh() != null) {
            vehicle.setBatteryCapacityKwh(request.getBatteryCapacityKwh());
        }
        if (request.getBatteryType() != null) {
            vehicle.setBatteryType(request.getBatteryType());
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
}

