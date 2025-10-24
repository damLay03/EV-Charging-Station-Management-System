package com.swp.evchargingstation.configuration;

import com.swp.evchargingstation.entity.ChargingPoint;
import com.swp.evchargingstation.repository.ChargingPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChargingPointNameBackfillRunner implements ApplicationRunner {

    private final ChargingPointRepository chargingPointRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<ChargingPoint> all = chargingPointRepository.findAll();
        if (all.isEmpty()) {
            return;
        }

        // Group charging points by station
        Map<String, List<ChargingPoint>> byStation = all.stream()
                .filter(cp -> cp.getStation() != null && cp.getStation().getStationId() != null)
                .collect(Collectors.groupingBy(cp -> cp.getStation().getStationId()));

        int totalUpdated = 0;
        for (Map.Entry<String, List<ChargingPoint>> entry : byStation.entrySet()) {
            String stationId = entry.getKey();
            List<ChargingPoint> points = entry.getValue();

            // Build set of used indices from existing names "TS{n}"
            Set<Integer> used = new HashSet<>();
            for (ChargingPoint cp : points) {
                String n = cp.getName();
                if (n != null && n.startsWith("TS")) {
                    try {
                        int idx = Integer.parseInt(n.substring(2));
                        if (idx > 0) used.add(idx);
                    } catch (NumberFormatException ignored) {
                        // ignore non-standard names
                    }
                }
            }

            // Sort unnamed points by pointId to have deterministic assignment
            List<ChargingPoint> unnamed = points.stream()
                    .filter(cp -> cp.getName() == null || cp.getName().isBlank())
                    .sorted(Comparator.comparing(ChargingPoint::getPointId, Comparator.nullsLast(String::compareTo)))
                    .toList();

            if (unnamed.isEmpty()) continue;

            // Assign smallest available indices sequentially
            for (ChargingPoint cp : unnamed) {
                int next = 1;
                while (used.contains(next)) next++;
                cp.setName("TS" + next);
                used.add(next);
                totalUpdated++;
                log.info("Backfilled charging point name for point {} at station {} -> {}", cp.getPointId(), stationId, cp.getName());
            }
        }

        if (totalUpdated > 0) {
            chargingPointRepository.saveAll(all);
            log.info("Backfilled {} charging point names across {} stations", totalUpdated, byStation.size());
        }
    }
}

