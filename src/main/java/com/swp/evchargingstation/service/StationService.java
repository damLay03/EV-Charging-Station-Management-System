package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.request.StationCreationRequest;
import com.swp.evchargingstation.dto.request.StationUpdateRequest;
import com.swp.evchargingstation.dto.request.ChargingPointCreationRequest;
import com.swp.evchargingstation.dto.response.StationDetailResponse;
import com.swp.evchargingstation.dto.response.StationResponse;
import com.swp.evchargingstation.dto.response.StaffSummaryResponse;
import com.swp.evchargingstation.dto.response.ChargingPointResponse;
import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.entity.ChargingSession;
import com.swp.evchargingstation.entity.Station;
import com.swp.evchargingstation.entity.Staff;
import com.swp.evchargingstation.enums.ChargingPointStatus;
import com.swp.evchargingstation.enums.StationStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.StationMapper;
import com.swp.evchargingstation.mapper.StaffMapper;
import com.swp.evchargingstation.mapper.ChargingPointMapper;
import com.swp.evchargingstation.repository.ChargingPointRepository;
import com.swp.evchargingstation.repository.ChargingSessionRepository;
import com.swp.evchargingstation.repository.PaymentRepository;
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
    ChargingPointMapper chargingPointMapper;
    ChargingSessionRepository chargingSessionRepository;
    PaymentRepository paymentRepository;
    GeocodingService geocodingService;

    /**
     * Tạo trạm sạc mới với số lượng điểm sạc và công suất chỉ định.
     * Tự động chuyển đổi địa chỉ thành tọa độ nếu không có sẵn.
     * @param request thông tin trạm cần tạo
     * @return StationResponse của trạm vừa tạo
     */
    @Transactional
    public StationResponse createStation(StationCreationRequest request) {
        log.info("Creating new station: {}", request.getName());

        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();

        // Nếu không có tọa độ, tự động geocode từ địa chỉ
        if (latitude == null || longitude == null) {
            log.info("No coordinates provided, geocoding address: {}", request.getAddress());
            var coordinates = geocodingService.geocodeAddress(request.getAddress());
            latitude = coordinates.get("latitude");
            longitude = coordinates.get("longitude");
            log.info("Geocoded coordinates: lat={}, lon={}", latitude, longitude);
        } else {
            // Validate tọa độ nếu có
            if (!geocodingService.isValidCoordinates(latitude, longitude)) {
                throw new AppException(ErrorCode.INVALID_COORDINATES);
            }
        }

        // Tạo station mới với trạng thái OUT_OF_SERVICE (chưa hoạt động)
        Station station = Station.builder()
                .name(request.getName())
                .address(request.getAddress())
                .operatorName(request.getOperatorName())
                .contactPhone(request.getContactPhone())
                .latitude(latitude)
                .longitude(longitude)
                .status(StationStatus.OUT_OF_SERVICE)
                .chargingPoints(new ArrayList<>())
                .build();

        // Gán staff nếu có
        if (request.getStaffId() != null && !request.getStaffId().isEmpty()) {
            Staff staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
            station.setStaff(staff);
            // Đồng bộ staff.station để mỗi staff gắn với một trạm
            staff.setStation(station);
            staffRepository.save(staff);
            log.info("Assigned staff {} to station (and set staff.station)", request.getStaffId());
        }

        // Lưu station trước để có ID
        Station savedStation = stationRepository.save(station);

        // Tạo các charging points cho station
        List<ChargingPoint> chargingPoints = new ArrayList<>();
        for (int i = 1; i <= request.getNumberOfChargingPoints(); i++) {
            ChargingPoint point = ChargingPoint.builder()
                    .station(savedStation)
                    .name("TS" + i)
                    .chargingPower(request.getPowerOutput())
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
     * Cập nhật thông tin cơ bản của một trạm (name, address, operatorName, contactPhone, status, staff).
     * Tự động chuyển đổi địa chỉ thành tọa độ nếu địa chỉ thay đổi và không có tọa độ mới.
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

        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();

        // Nếu địa chỉ thay đổi nhưng không có tọa độ mới, tự động geocode
        if (!request.getAddress().equals(station.getAddress()) &&
            (latitude == null || longitude == null)) {
            log.info("Address changed without coordinates, geocoding new address: {}", request.getAddress());
            var coordinates = geocodingService.geocodeAddress(request.getAddress());
            latitude = coordinates.get("latitude");
            longitude = coordinates.get("longitude");
            log.info("Geocoded new coordinates: lat={}, lon={}", latitude, longitude);
        } else if (latitude != null && longitude != null) {
            // Validate tọa độ nếu có
            if (!geocodingService.isValidCoordinates(latitude, longitude)) {
                throw new AppException(ErrorCode.INVALID_COORDINATES);
            }
        } else {
            // Giữ nguyên tọa độ cũ nếu địa chỉ không đổi và không có tọa độ mới
            latitude = station.getLatitude();
            longitude = station.getLongitude();
        }

        // Update fields
        station.setName(request.getName());
        station.setAddress(request.getAddress());
        station.setOperatorName(request.getOperatorName());
        station.setContactPhone(request.getContactPhone());
        station.setLatitude(latitude);
        station.setLongitude(longitude);
        station.setStatus(request.getStatus());

        // Update staff nếu có trong request
        if (request.getStaffId() != null) {
            if (request.getStaffId().isEmpty()) {
                // Nếu staffId = "" thì bỏ gán staff
                if (station.getStaff() != null) {
                    Staff prev = station.getStaff();
                    station.setStaff(null);
                    if (prev.getStation() != null && stationId.equals(prev.getStation().getStationId())) {
                        prev.setStation(null);
                        staffRepository.save(prev);
                    }
                    log.info("Removed staff from station {} and cleared prevStaff.station", stationId);
                } else {
                    log.info("Station {} already has no staff assigned", stationId);
                }
            } else {
                // Gán staff mới
                Staff staff = staffRepository.findById(request.getStaffId())
                        .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));
                station.setStaff(staff);
                // Đồng bộ staff.station
                staff.setStation(station);
                staffRepository.save(staff);
                log.info("Updated staff of station {} to {} and set staff.station", stationId, request.getStaffId());
            }
        }

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
     * Nếu status != null => lọc theo trạng thái chỉ ��ịnh.
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
     * Danh sách tất cả staff có thể gán cho station.
     * Không giới hạn staff đã được gán hay chưa.
     */
    @Transactional(readOnly = true)
    public List<StaffSummaryResponse> getAllStaff() {
        log.info("Fetching all staff for station assignment");
        return staffRepository.findAll().stream()
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
        Integer inUsePoints = chargingPointRepository.countByStationIdAndStatus(stationId, ChargingPointStatus.CHARGING);
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
        String staffName = null;
        String staffId = null;
        if (station.getStaff() != null) {
            Staff staff = station.getStaff();
            staffId = staff.getUserId();
            if (staff.getUser() != null) {
                staffName = staff.getUser().getFullName();
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
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .status(station.getStatus())
                .totalChargingPoints(totalPoints)
                .activeChargingPoints(activePoints)
                .offlineChargingPoints(offlinePoints)
                .maintenanceChargingPoints(maintenancePoints)
                .chargingPointsSummary(chargingPointsSummary)
                .revenue(revenue)
                .usagePercent(usagePercent)
                .staffId(staffId)
                .staffName(staffName)
                .build();
    }

    /**
     * Tạo thêm trụ sạc cho trạm sạc đã tồn tại.
     * Trụ sạc mới sẽ được tạo với trạng thái AVAILABLE nếu không chỉ định.
     * @param stationId id của trạm cần thêm trụ sạc
     * @param request thông tin trụ sạc cần tạo
     * @return ChargingPointResponse của trụ sạc vừa tạo
     * @throws AppException nếu không tìm thấy trạm
     */
    @Transactional
    public ChargingPointResponse addChargingPointToStation(String stationId, ChargingPointCreationRequest request) {
        log.info("Adding charging point to station {} with power {}", stationId, request.getChargingPower());

        // Kiểm tra station có tồn tại không
        Station station = stationRepository.findById(stationId)
                .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));

        // Xác định trạng thái - mặc định là AVAILABLE nếu không truyền
        ChargingPointStatus status = request.getStatus() != null ? request.getStatus() : ChargingPointStatus.AVAILABLE;

        // Sinh tên trụ sạc tiếp theo theo thứ tự TS{n} dựa trên số lớn nhất hiện có (phòng trường hợp đã xóa một số trụ)
        List<ChargingPoint> existingPoints = chargingPointRepository.findByStation_StationId(stationId);
        int maxIndex = 0;
        for (ChargingPoint cp : existingPoints) {
            String n = cp.getName();
            if (n != null && n.startsWith("TS")) {
                try {
                    int idx = Integer.parseInt(n.substring(2));
                    if (idx > maxIndex) maxIndex = idx;
                } catch (NumberFormatException ignored) {
                    // Bỏ qua các tên không theo định dạng TS{number}
                }
            }
        }
        String nextName = "TS" + (maxIndex + 1);

        // Tạo charging point mới
        ChargingPoint newPoint = ChargingPoint.builder()
                .station(station)
                .name(nextName)
                .chargingPower(request.getChargingPower())
                .status(status)
                .build();

        // Lưu charging point
        ChargingPoint savedPoint = chargingPointRepository.save(newPoint);

        log.info("Created charging point {} ({}) for station {}", savedPoint.getPointId(), nextName, stationId);

        // Trả về response
        return chargingPointMapper.toChargingPointResponse(savedPoint);
    }

    /**
     * Lấy danh sách tất cả trụ sạc của một trạm sạc.
     * @param stationId id của trạm
     * @return danh sách ChargingPointResponse
     * @throws AppException nếu không tìm thấy trạm
     */
    @Transactional(readOnly = true)
    public List<ChargingPointResponse> getChargingPointsByStation(String stationId) {
        log.info("Fetching charging points for station {}", stationId);

        // Kiểm tra station có tồn tại không
        if (!stationRepository.existsById(stationId)) {
            throw new AppException(ErrorCode.STATION_NOT_FOUND);
        }

        // Lấy danh sách charging points
        List<ChargingPoint> chargingPoints = chargingPointRepository.findByStation_StationId(stationId);

        return chargingPoints.stream()
                .map(chargingPointMapper::toChargingPointResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật thông tin trụ sạc.
     * @param stationId id của trạm sạc chứa trụ sạc
     * @param pointId id của trụ sạc cần cập nhật
     * @param request thông tin cập nhật
     * @return ChargingPointResponse sau khi cập nhật
     * @throws AppException nếu không tìm thấy trụ sạc hoặc trụ sạc không thuộc trạm
     */
    @Transactional
    public ChargingPointResponse updateChargingPoint(String stationId, String pointId, com.swp.evchargingstation.dto.request.ChargingPointUpdateRequest request) {
        log.info("Updating charging point {} at station {}", pointId, stationId);

        // Kiểm tra charging point có tồn tại không
        ChargingPoint chargingPoint = chargingPointRepository.findById(pointId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));

        // Kiểm tra charging point có thuộc station này không
        if (!chargingPoint.getStation().getStationId().equals(stationId)) {
            log.warn("Charging point {} does not belong to station {}", pointId, stationId);
            throw new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND);
        }

        // Cập nhật chargingPower nếu có
        if (request.getChargingPower() != null) {
            chargingPoint.setChargingPower(request.getChargingPower());
            log.info("Updated power for charging point {} to {}", pointId, request.getChargingPower());
        }

        // Cập nhật status nếu có
        if (request.getStatus() != null) {
            chargingPoint.setStatus(request.getStatus());
            log.info("Updated status for charging point {} to {}", pointId, request.getStatus());
        }

        // Lưu charging point
        ChargingPoint savedPoint = chargingPointRepository.save(chargingPoint);

        log.info("Updated charging point {} successfully", pointId);

        return chargingPointMapper.toChargingPointResponse(savedPoint);
    }

    /**
     * Xóa trụ sạc theo id.
     * @param stationId id của trạm sạc chứa trụ sạc
     * @param pointId id của trụ sạc cần xóa
     * @throws AppException nếu không tìm thấy trụ sạc, trụ sạc không thuộc trạm, hoặc trụ sạc đang có session
     */
    @Transactional
    public void deleteChargingPoint(String stationId, String pointId) {
        log.info("Deleting charging point {} from station {}", pointId, stationId);

        // Kiểm tra charging point có tồn tại không
        ChargingPoint chargingPoint = chargingPointRepository.findById(pointId)
                .orElseThrow(() -> new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND));

        // Kiểm tra charging point có thuộc station này không
        if (!chargingPoint.getStation().getStationId().equals(stationId)) {
            log.warn("Charging point {} does not belong to station {}", pointId, stationId);
            throw new AppException(ErrorCode.CHARGING_POINT_NOT_FOUND);
        }

        // Kiểm tra xem có session đang hoạt động không
        if (chargingPoint.getCurrentSession() != null) {
            log.warn("Cannot delete charging point {} - has active session", pointId);
            throw new AppException(ErrorCode.CHARGING_POINT_IN_USE);
        }

        // Xóa charging point
        chargingPointRepository.delete(chargingPoint);
        log.info("Deleted charging point {} successfully", pointId);
    }

    /**
     * STAFF: Lấy tất cả trụ sạc của trạm mà nhân viên đang làm việc.
     * Trả về danh sách rỗng nếu staff chưa được gán trạm (để FE hiển thị "chưa có dữ liệu").
     */
    @Transactional(readOnly = true)
    public List<ChargingPointResponse> getMyStationChargingPoints(String staffId) {
        log.info("Staff {} fetching charging points of their station", staffId);
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        if (staff.getStation() == null) {
            log.info("Staff {} has no station assigned. Returning empty list.", staffId);
            return List.of();
        }

        String stationId = staff.getStation().getStationId();
        List<ChargingPoint> points = chargingPointRepository.findByStation_StationId(stationId);
        return points.stream()
                .map(chargingPointMapper::toChargingPointResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy lịch sử thanh toán của một trạm sạc.
     * ADMIN: Có thể xem lịch sử của bất kỳ trạm nào.
     * STAFF: Chỉ được xem lịch sử của trạm mình quản lý.
     *
     * @param stationId ID của trạm cần xem lịch sử
     * @param startDate Ngày bắt đầu filter (nullable)
     * @param endDate Ngày kết thúc filter (nullable)
     * @param paymentMethod Phương thức thanh toán filter (nullable)
     * @return Danh sách lịch sử thanh toán
     */
    @Transactional(readOnly = true)
    public List<com.swp.evchargingstation.dto.response.PaymentHistoryResponse> getPaymentHistory(
            String stationId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            com.swp.evchargingstation.entity.Payment.PaymentMethod paymentMethod) {

        log.info("Fetching payment history for station {} - startDate: {}, endDate: {}, paymentMethod: {}",
                stationId, startDate, endDate, paymentMethod);

        // Kiểm tra station có tồn tại không
        if (!stationRepository.existsById(stationId)) {
            throw new AppException(ErrorCode.STATION_NOT_FOUND);
        }

        // Convert LocalDate to LocalDateTime (start of day / end of day)
        java.time.LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        java.time.LocalDateTime endDateTime = endDate != null ? endDate.atTime(23, 59, 59) : null;

        // Lấy danh sách payments
        List<com.swp.evchargingstation.entity.Payment> payments =
                paymentRepository.findPaymentHistoryByStationId(stationId, startDateTime, endDateTime, paymentMethod);

        // Map sang response DTO
        return payments.stream()
                .map(this::mapToPaymentHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper method: Map Payment entity sang PaymentHistoryResponse
     */
    private com.swp.evchargingstation.dto.response.PaymentHistoryResponse mapToPaymentHistoryResponse(
            com.swp.evchargingstation.entity.Payment payment) {

        ChargingSession session = payment.getChargingSession();
        String customerName = null;
        String chargingPointName = null;
        Integer durationMinutes = null;
        String durationFormatted = null;

        if (session != null) {
            // Lấy tên khách hàng
            if (session.getDriver() != null && session.getDriver().getUser() != null) {
                customerName = session.getDriver().getUser().getFullName();
            }

            // Lấy tên điểm sạc
            if (session.getChargingPoint() != null) {
                chargingPointName = "Điểm sạc " + session.getChargingPoint().getName();
            }

            // Tính thời gian sạc
            durationMinutes = session.getDurationMin();
            durationFormatted = formatDuration(durationMinutes);
        }

        // Format payment method display
        String paymentMethodDisplay = formatPaymentMethodDisplay(payment.getPaymentMethod());

        return com.swp.evchargingstation.dto.response.PaymentHistoryResponse.builder()
                .paymentId(payment.getPaymentId())
                .paymentTime(payment.getPaidAt())
                .chargingPointName(chargingPointName)
                .customerName(customerName)
                .durationMinutes(durationMinutes)
                .durationFormatted(durationFormatted)
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentMethodDisplay(paymentMethodDisplay)
                .sessionId(session != null ? session.getSessionId() : null)
                .build();
    }

    /**
     * Format duration từ phút sang dạng "35 phút" hoặc "1h 15m"
     */
    private String formatDuration(Integer minutes) {
        if (minutes == null || minutes == 0) {
            return "0 phút";
        }

        int hours = minutes / 60;
        int mins = minutes % 60;

        if (hours == 0) {
            return mins + " phút";
        } else if (mins == 0) {
            return hours + "h";
        } else {
            return hours + "h " + mins + "m";
        }
    }

    /**
     * Format payment method display name
     */
    private String formatPaymentMethodDisplay(com.swp.evchargingstation.entity.Payment.PaymentMethod method) {
        if (method == null) {
            return "N/A";
        }

        switch (method) {
            case CASH:
                return "Tiền mặt";
            case ZALOPAY:
                return "ZaloPay";
            default:
                return method.name();
        }
    }
}
