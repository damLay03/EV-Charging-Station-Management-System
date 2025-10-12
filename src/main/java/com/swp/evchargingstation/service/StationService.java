package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.StationCreationRequest;
import com.swp.evchargingstation.dto.request.StationUpdateRequest;
import com.swp.evchargingstation.dto.response.StationDetailResponse;
import com.swp.evchargingstation.dto.response.StationResponse;
import com.swp.evchargingstation.dto.response.StaffSummaryResponse;
import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.entity.Station;
import com.swp.evchargingstation.entity.Staff;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.StationStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.StationMapper;
import com.swp.evchargingstation.mapper.StaffMapper;
import com.swp.evchargingstation.repository.ChargingPointRepository;
import com.swp.evchargingstation.repository.ChargingSessionRepository;
import com.swp.evchargingstation.repository.StationRepository;
import com.swp.evchargingstation.repository.StaffRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StationService {
    StationRepository stationRepository;
    StationMapper stationMapper;
    StaffRepository staffRepository;
    StaffMapper staffMapper;
    ChargingPointRepository chargingPointRepository;
    ChargingSessionRepository chargingSessionRepository;

    /**
     * Tạo trạm sạc mới với số lượng điểm sạc và công suất chỉ định.
     * @param request thông tin trạm cần tạo
     * @return StationResponse của trạm vừa tạo
     */
    @Transactional
    public StationResponse createStation(StationCreationRequest request) {
        log.info("Creating new station: {}", request.getName());

        // Tạo station mới với trạng thái OUT_OF_SERVICE (chưa hoạt động)
        Station station = Station.builder()
                .name(request.getName())
                .address(request.getAddress())
                .operatorName(request.getOperatorName())
                .contactPhone(request.getContactPhone())
                .status(StationStatus.OUT_OF_SERVICE)
                .chargingPoints(new ArrayList<>())
                .build();

        // Lưu station trước để có ID
        Station savedStation = stationRepository.save(station);

        // Tạo các charging points cho station
        List<ChargingPoint> chargingPoints = new ArrayList<>();
        for (int i = 1; i <= request.getNumberOfChargingPoints(); i++) {
            ChargingPoint point = ChargingPoint.builder()
                    .station(savedStation)
                    .maxPowerKw(request.getPowerOutputKw())
                    .status(ChargingPointStatus.AVAILABLE)
                    .build();
            chargingPoints.add(point);
        }

        // Lưu tất cả charging points
        chargingPointRepository.saveAll(chargingPoints);
        savedStation.setChargingPoints(chargingPoints);

        log.info("Created station {} with {} charging points", savedStation.getStationId(), chargingPoints.size());
        return stationMapper.toStationResponse(savedStation);
    }

    /**
     * Cập nhật thông tin cơ bản của một trạm (name, address, operatorName, contactPhone, status).
     * Không thay đổi số lượng charging points hoặc cấu hình phần cứng.
     * @param stationId id của trạm cần cập nhật
     * @param request thông tin cập nhật
     * @return StationResponse sau khi cập nhật
     * @throws AppException nếu không tìm thấy trạm
     */
    @Transactional
    public StationResponse updateStation(String stationId, StationUpdateRequest request) {
        log.info("Updating station id='{}' with name='{}'", stationId, request.getName());
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));

        // Update fields
        station.setName(request.getName());
        station.setAddress(request.getAddress());
        station.setOperatorName(request.getOperatorName());
        station.setContactPhone(request.getContactPhone());
        station.setStatus(request.getStatus());

        Station saved = stationRepository.save(station);
        log.info("Updated station {} successfully", stationId);
        return stationMapper.toStationResponse(saved);
    }

    /**
     * Xóa trạm sạc theo id.
     * Xóa luôn tất cả charging points liên quan (cascade).
     * @param stationId id trạm cần xóa
     * @throws AppException nếu không tìm thấy trạm
     */
    @Transactional
    public void deleteStation(String stationId) {
        log.info("Deleting station id='{}'", stationId);
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));

        // Xóa station (charging points sẽ tự động xóa do cascade = CascadeType.ALL)
        stationRepository.delete(station);
        log.info("Deleted station '{}' successfully", stationId);
    }

    /**
     * Lấy danh sách trạm.
     * Nếu status == null => trả về tất cả.
     * Nếu status != null => lọc theo trạng thái chỉ định.
     * @param status trạng thái cần lọc (có thể null)
     * @return danh sách StationResponse
     */
    @Transactional(readOnly = true)
    public List<StationResponse> getStations(StationStatus status) {
        log.info("Fetching stations with status: {}", status);
        List<Station> stations = (status == null) ? stationRepository.findAll() : stationRepository.findByStatus(status);
        return stations.stream().map(stationMapper::toStationResponse).toList();
    }

    /**
     * Lấy danh sách trạm theo cờ active.
     * active = true  => chỉ lấy các trạm status = OPERATIONAL.
     * active = false => lấy các trạm khác OPERATIONAL.
     * active = null  => tương đương getStations(null).
     * @param active cờ lọc theo trạng thái hoạt động (có thể null)
     * @return danh sách StationResponse theo cờ active
     */
    @Transactional(readOnly = true)
    public List<StationResponse> getStationsByActive(Boolean active) {
        if (active == null) {
            return getStations(null);
        }
        log.info("Fetching stations with active flag: {}", active);
        List<Station> stations = stationRepository.findAll().stream()
                .filter(s -> active ? s.getStatus() == StationStatus.OPERATIONAL : s.getStatus() != StationStatus.OPERATIONAL)
                .toList();
        return stations.stream().map(stationMapper::toStationResponse).toList();
    }

    /**
     * Cập nhật trạng thái của một trạm sang giá trị bất kỳ trong enum StationStatus.
     * @param stationId id của trạm
     * @param status trạng thái mới
     * @return StationResponse sau khi cập nhật
     * @throws AppException nếu không tìm thấy trạm
     */
    @Transactional
    public StationResponse updateStationStatus(String stationId, StationStatus status) {
        log.info("Updating status of station {} to {}", stationId, status);
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));
        station.setStatus(status);
        Station saved = stationRepository.save(station);
        return stationMapper.toStationResponse(saved);
    }

    /**
     * Kích hoạt trạm: set trạng thái về OPERATIONAL.
     * @param stationId id trạm
     * @return StationResponse sau khi cập nhật
     */
    @Transactional
    public StationResponse activate(String stationId) {
        log.info("Activating station {}", stationId);
        return updateStationStatus(stationId, StationStatus.OPERATIONAL);
    }

    /**
     * Ngưng hoạt động trạm: set trạng thái OUT_OF_SERVICE.
     * @param stationId id trạm
     * @return StationResponse sau khi cập nhật
     */
    @Transactional
    public StationResponse deactivate(String stationId) {
        log.info("Deactivating station {} (set OUT_OF_SERVICE)", stationId);
        return updateStationStatus(stationId, StationStatus.OUT_OF_SERVICE);
    }

    /**
     * Chuyển đổi trạng thái giữa OPERATIONAL <-> OUT_OF_SERVICE.
     * (Không tác động tới các trạng thái khác: MAINTENANCE, CLOSED.)
     * @param stationId id trạm
     * @return StationResponse sau khi toggle
     */
    @Transactional
    public StationResponse toggle(String stationId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));
        StationStatus newStatus = station.getStatus() == StationStatus.OPERATIONAL ? StationStatus.OUT_OF_SERVICE : StationStatus.OPERATIONAL;
        log.info("Toggling station {} from {} to {}", stationId, station.getStatus(), newStatus);
        station.setStatus(newStatus);
        return stationMapper.toStationResponse(stationRepository.save(station));
    }

    /**
     * Lấy danh sách tổng quan (overview) tất cả trạm.
     * Dùng cho FE hiển thị nhanh bảng tổng quan (ít field hơn StationResponse đầy đủ).
     * @return danh sách overview với cờ active
     */
    @Transactional(readOnly = true)
    public List<com.swp.evchargingstation.dto.response.StationOverviewResponse> getAllOverview() {
        log.info("Fetching overview list of all stations");
        return stationRepository.findAll().stream()
                .map(stationMapper::toStationOverviewResponse)
                .toList();
    }

    /**
     * Danh sách nhân viên đã gán cho một trạm.
     */
    @Transactional(readOnly = true)
    public List<StaffSummaryResponse> getStaffOfStation(String stationId) {
        // đảm bảo trạm tồn tại
        stationRepository.findById(stationId)
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));
        return staffRepository.findByStation_StationId(stationId).stream()
                .map(staffMapper::toStaffSummaryResponse)
                .toList();
    }

    /**
     * Gán một nhân viên (staff) vào trạm.
     * Rule: nếu staff đã thuộc 1 trạm khác -> STAFF_ALREADY_ASSIGNED (không tự động chuyển trạm để tránh nhầm lẫn).
     */
    @Transactional
    public StaffSummaryResponse assignStaff(String stationId, String staffId) {
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        if (staff.getStation() != null) {
            if (stationId.equals(staff.getStation().getStationId())) {
                throw new AppException(ErrorCode.STAFF_ALREADY_ASSIGNED); // đã gán đúng trạm rồi
            }
            // đang gán trạm khác -> không cho (business hiện tại)
            throw new AppException(ErrorCode.STAFF_ALREADY_ASSIGNED);
        }
        staff.setStation(station);
        Staff saved = staffRepository.save(staff);
        return staffMapper.toStaffSummaryResponse(saved);
    }

    /**
     * Bỏ gán nhân viên khỏi trạm.
     */
    @Transactional
    public void unassignStaff(String stationId, String staffId) {
        stationRepository.findById(stationId)
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
        if (staff.getStation() == null || !stationId.equals(staff.getStation().getStationId())) {
            throw new AppException(ErrorCode.STAFF_NOT_IN_STATION);
        }
        staff.setStation(null);
        staffRepository.save(staff);
    }

    /**
     * Danh sách nhân viên chưa được gán vào trạm nào.
     */
    @Transactional(readOnly = true)
    public List<StaffSummaryResponse> getUnassignedStaff() {
        return staffRepository.findByStationIsNull().stream()
                .map(staffMapper::toStaffSummaryResponse)
                .toList();
    }

    /**
     * Lấy danh sách trạm với thông tin chi tiết (bao gồm charging points, revenue, usage, staff).
     * Có thể lọc theo status nếu cần.
     * @param status trạng thái cần lọc (có thể null)
     * @return danh sách StationDetailResponse
     */
    @Transactional(readOnly = true)
    public List<StationDetailResponse> getStationsWithDetail(StationStatus status) {
        log.info("Fetching stations with detail - status: {}", status);
        List<Station> stations = (status == null)
                ? stationRepository.findAll()
                : stationRepository.findByStatus(status);

        return stations.stream()
                .map(this::mapToStationDetailResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper method: map Station sang StationDetailResponse với thông tin đầy đủ.
     * Nguyên tắc: Field nào không có dữ liệu thật → NULL, không tự chế!
     */
    private StationDetailResponse mapToStationDetailResponse(Station station) {
        String stationId = station.getStationId();

        // Đếm số lượng charging points theo trạng thái - GIỮ NGUYÊN NULL nếu không có
        Integer totalPoints = chargingPointRepository.countByStationId(stationId);
        Integer availablePoints = chargingPointRepository.countByStationIdAndStatus(stationId, ChargingPointStatus.AVAILABLE);
        Integer inUsePoints = chargingPointRepository.countByStationIdAndStatus(stationId, ChargingPointStatus.OCCUPIED);
        Integer offlinePoints = chargingPointRepository.countByStationIdAndStatus(stationId, ChargingPointStatus.OUT_OF_SERVICE);
        Integer maintenancePoints = chargingPointRepository.countByStationIdAndStatus(stationId, ChargingPointStatus.MAINTENANCE);

        // Tính activePoints - CHỈ tính nếu CÓ dữ liệu, không tự chế
        Integer activePoints = null;
        if (availablePoints != null && inUsePoints != null) {
            activePoints = availablePoints + inUsePoints;
        } else if (availablePoints != null) {
            activePoints = availablePoints;
        } else if (inUsePoints != null) {
            activePoints = inUsePoints;
        }

        // Tính doanh thu từ charging sessions - GIỮ NULL nếu không có
        Double revenue = chargingSessionRepository.sumRevenueByStationId(stationId);

        // Tính phần trăm sử dụng - CHỈ tính nếu có dữ liệu đầy đủ
        Double usagePercent = null;
        if (totalPoints != null && totalPoints > 0 && inUsePoints != null) {
            usagePercent = (inUsePoints * 100.0 / totalPoints);
        }

        // Lấy tên nhân viên - CHỈ lấy nếu CÓ, không có thì NULL
        List<Staff> staffList = staffRepository.findByStation_StationId(stationId);
        String staffName = null;
        if (staffList != null && !staffList.isEmpty()) {
            Staff firstStaff = staffList.get(0);
            if (firstStaff != null && firstStaff.getUser() != null) {
                staffName = firstStaff.getUser().getFullName();
            }
        }

        // Tạo summary string - CHỈ tạo nếu có dữ liệu
        String chargingPointsSummary = null;
        if (totalPoints != null || activePoints != null || offlinePoints != null || maintenancePoints != null) {
            chargingPointsSummary = String.format("Tổng: %s | Hoạt động: %s | Offline: %s | Bảo trì: %s",
                    totalPoints != null ? totalPoints : "-",
                    activePoints != null ? activePoints : "-",
                    offlinePoints != null ? offlinePoints : "-",
                    maintenancePoints != null ? maintenancePoints : "-");
        }

        return StationDetailResponse.builder()
                .stationId(stationId)
                .name(station.getName())
                .address(station.getAddress())
                .status(station.getStatus())
                .totalChargingPoints(totalPoints) // NULL nếu không có
                .activeChargingPoints(activePoints) // NULL nếu không có
                .offlineChargingPoints(offlinePoints) // NULL nếu không có
                .maintenanceChargingPoints(maintenancePoints) // NULL nếu không có
                .chargingPointsSummary(chargingPointsSummary) // NULL nếu không có dữ liệu
                .revenue(revenue) // NULL nếu không có
                .usagePercent(usagePercent) // NULL nếu không có đủ dữ liệu để tính
                .staffName(staffName) // NULL nếu không có nhân viên
                .build();
    }
}
