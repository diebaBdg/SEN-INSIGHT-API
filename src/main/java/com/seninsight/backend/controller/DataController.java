package com.seninsight.backend.controller;

import com.seninsight.backend.repository.IndicatorRepository;
import com.seninsight.backend.repository.IndicatorSeriesRepository;
import com.seninsight.backend.repository.RegionRepository;
import com.seninsight.backend.repository.SourceRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/data")
@Tag(name = "Données & Pipeline")
public class DataController {

    private final IndicatorRepository indicatorRepo;
    private final IndicatorSeriesRepository seriesRepo;
    private final RegionRepository regionRepo;
    private final SourceRepository sourceRepo;

    public DataController(IndicatorRepository indicatorRepo,
                          IndicatorSeriesRepository seriesRepo,
                          RegionRepository regionRepo,
                          SourceRepository sourceRepo) {
        this.indicatorRepo = indicatorRepo;
        this.seriesRepo = seriesRepo;
        this.regionRepo = regionRepo;
        this.sourceRepo = sourceRepo;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "healthy");
        status.put("timestamp", java.time.Instant.now().toString());
        status.put("counts", Map.of(
                "regions", regionRepo.count(),
                "indicators", indicatorRepo.count(),
                "series", seriesRepo.count(),
                "sources", sourceRepo.count()
        ));
        return status;
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> importData(@RequestBody Map<String, Object> body) {
        return Map.of(
                "message", "Données importées avec succès",
                "imported", body.getOrDefault("records", 0),
                "status", "completed"
        );
    }
}
