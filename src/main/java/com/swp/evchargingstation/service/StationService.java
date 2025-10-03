package com.swp.evchargingstation.service;

import com.swp.evchargingstation.dto.response.StationResponse;
import com.swp.evchargingstation.dto.response.StaffSummaryResponse;
import com.swp.evchargingstation.entity.Station;
import com.swp.evchargingstation.entity.Staff;
import com.swp.evchargingstation.enums.StationStatus;
import com.swp.evchargingstation.exception.AppException;
import com.swp.evchargingstation.exception.ErrorCode;
import com.swp.evchargingstation.mapper.StationMapper;
import com.swp.evchargingstation.mapper.StaffMapper;
import com.swp.evchargingstation.repository.StationRepository;
import com.swp.evchargingstation.repository.StaffRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StationService {
    StationRepository stationRepository;
    StationMapper stationMapper;
    // NOTE: Thêm repository + mapper cho chức năng phân công nhân viên
    StaffRepository staffRepository;
    StaffMapper staffMapper;

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
}
