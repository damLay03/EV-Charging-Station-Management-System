package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.IncidentCreationRequest;
import com.swp.evchargingstation.dto.request.IncidentUpdateRequest;
import com.swp.evchargingstation.dto.response.ApiResponse;
import com.swp.evchargingstation.dto.response.IncidentResponse;
import com.swp.evchargingstation.entity.*;
import com.swp.evchargingstation.enums.IncidentStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.StaffDashboardMapper;
import com.swp.evchargingstation.repository.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class IncidentService {

    IncidentRepository incidentRepository;
    StaffRepository staffRepository;
    UserRepository userRepository;
    ChargingPointRepository chargingPointRepository;
    StaffDashboardMapper staffDashboardMapper;
    CloudinaryService cloudinaryService;

    /**
     * Get current user ID from JWT token
     */
    private String getCurrentUserId() {
        var context = SecurityContextHolder.getContext();
        var authentication = context.getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getClaim("userId");
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    /**
     * Check if current user is ADMIN
     */
    private boolean isAdmin() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    //=====================================================STAFF============================================================

    /**
     * STAFF: Tạo báo cáo sự cố tại station của mình
     * Trạng thái mặc định: WAITING (chờ admin duyệt)
     */
    @Transactional
    public ApiResponse<IncidentResponse> createIncident(IncidentCreationRequest request, MultipartFile image) {
        String staffUserId = getCurrentUserId();

        Staff staff = staffRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        User staffUser = userRepository.findById(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Station station = staff.getStation();
        if (station == null) {
            throw new AppException(ErrorCode.STATION_NOT_FOUND);
        }

        // Validate station ID matches staff's station
        if (!station.getStationId().equals(request.getStationId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ChargingPoint chargingPoint = null;
        if (request.getChargingPointId() != null) {
            chargingPoint = chargingPointRepository.findById(request.getChargingPointId())
                    .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));
        }

        // Upload image if provided
        String imageUrl = null;
        if (image != null) {
            imageUrl = cloudinaryService.uploadIncidentImage(image);
        }

        Incident incident = Incident.builder()
                .reporter(staffUser)
                .station(station)
                .chargingPoint(chargingPoint)
                .reportedAt(LocalDateTime.now())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .status(IncidentStatus.WAITING)  // Mặc định là WAITING
                .assignedStaff(staff)
                .imageUrl(imageUrl)
                .build();

        incident = incidentRepository.save(incident);

        log.info("Staff {} created incident {} with status WAITING", staffUserId, incident.getIncidentId());

        return ApiResponse.<IncidentResponse>builder()
                .code(200)
                .message("Báo cáo sự cố thành công, đang chờ admin duyệt")
                .result(staffDashboardMapper.toIncidentResponse(incident))
                .build();
    }

    /**
     * STAFF: Xem danh sách incidents của station mình quản lý
     */
    public List<IncidentResponse> getMyStationIncidents() {
        String staffUserId = getCurrentUserId();

        Staff staff = staffRepository.findByIdWithStation(staffUserId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        Station station = staff.getStation();
        if (station == null) {
            throw new AppException(ErrorCode.STATION_NOT_FOUND);
        }

        List<Incident> incidents = incidentRepository.findByStationIdOrderByReportedAtDesc(station.getStationId());

        return incidents.stream()
                .map(staffDashboardMapper::toIncidentResponse)
                .collect(Collectors.toList());
    }

    /**
     * STAFF/ADMIN: Cập nhật incident
     * - STAFF: chỉ có thể cập nhật description của incident tại station của mình
     * - ADMIN: có thể cập nhật cả description và status của bất kỳ incident nào
     * - Cả STAFF và ADMIN đều có thể cập nhật ảnh (thay thế ảnh cũ)
     */
    @Transactional
    public ApiResponse<IncidentResponse> updateIncident(String incidentId, IncidentUpdateRequest request, MultipartFile image) {
        String userId = getCurrentUserId();
        boolean isAdminUser = isAdmin();

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        if (isAdminUser) {
            // ADMIN: có thể cập nhật cả description và status
            if (request.getDescription() != null) {
                incident.setDescription(request.getDescription());
            }

            if (request.getStatus() != null) {
                incident.setStatus(request.getStatus());

                // Nếu status là DONE thì set resolvedAt
                if (request.getStatus() == IncidentStatus.DONE) {
                    incident.setResolvedAt(LocalDateTime.now());
                }
            }

            // Update image if provided
            if (image != null) {
                // Delete old image
                if (incident.getImageUrl() != null) {
                    cloudinaryService.deleteImage(incident.getImageUrl());
                }
                // Upload new image
                String newImageUrl = cloudinaryService.uploadIncidentImage(image);
                incident.setImageUrl(newImageUrl);
            }

            incident = incidentRepository.save(incident);

            log.info("Admin {} updated incident {}", userId, incidentId);

            return ApiResponse.<IncidentResponse>builder()
                    .code(200)
                    .message("Cập nhật sự cố thành công")
                    .result(staffDashboardMapper.toIncidentResponse(incident))
                    .build();
        } else {
            // STAFF: chỉ có thể cập nhật description của incident tại station của mình
            Staff staff = staffRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

            // Kiểm tra incident thuộc station của staff
            if (!incident.getStation().getStationId().equals(staff.getStation().getStationId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            // Staff không được phép thay đổi status
            if (request.getStatus() != null) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }

            // Staff chỉ có thể cập nhật description
            if (request.getDescription() != null) {
                incident.setDescription(request.getDescription());
            }

            // Update image if provided
            if (image != null) {
                // Delete old image
                if (incident.getImageUrl() != null) {
                    cloudinaryService.deleteImage(incident.getImageUrl());
                }
                // Upload new image
                String newImageUrl = cloudinaryService.uploadIncidentImage(image);
                incident.setImageUrl(newImageUrl);
            }

            incident = incidentRepository.save(incident);

            log.info("Staff {} updated incident {} description", userId, incidentId);

            return ApiResponse.<IncidentResponse>builder()
                    .code(200)
                    .message("Cập nhật mô tả sự cố thành công")
                    .result(staffDashboardMapper.toIncidentResponse(incident))
                    .build();
        }
    }

    //=====================================================ADMIN============================================================

    /**
     * ADMIN: Xem tất cả incidents của tất cả stations
     */
    public List<IncidentResponse> getAllIncidents() {
        List<Incident> incidents = incidentRepository.findAllByOrderByReportedAtDesc();

        return incidents.stream()
                .map(staffDashboardMapper::toIncidentResponse)
                .collect(Collectors.toList());
    }

    /**
     * ADMIN: Xem chi tiết một incident
     */
    public ApiResponse<IncidentResponse> getIncidentById(String incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        return ApiResponse.<IncidentResponse>builder()
                .result(staffDashboardMapper.toIncidentResponse(incident))
                .build();
    }

    /**
     * ADMIN: Xóa incident
     */
    @Transactional
    public ApiResponse<Void> deleteIncident(String incidentId) {
        String adminUserId = getCurrentUserId();

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new AppException(ErrorCode.INCIDENT_NOT_FOUND));

        // Delete image from Cloudinary
        if (incident.getImageUrl() != null) {
            cloudinaryService.deleteImage(incident.getImageUrl());
        }

        incidentRepository.delete(incident);

        log.info("Admin {} deleted incident {}", adminUserId, incidentId);

        return ApiResponse.<Void>builder()
                .message("Xóa báo cáo sự cố thành công")
                .build();
    }
}
