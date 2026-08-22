package com.seninsight.backend.controller;

import com.seninsight.backend.entity.IndicatorSeries;
import com.seninsight.backend.entity.Region;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.IndicatorRepository;
import com.seninsight.backend.repository.IndicatorSeriesRepository;
import com.seninsight.backend.repository.RegionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/export")
@Tag(name = "Export & Données brutes")
public class ExportController {

    private final IndicatorRepository indicatorRepo;
    private final IndicatorSeriesRepository seriesRepo;
    private final RegionRepository regionRepo;

    public ExportController(IndicatorRepository indicatorRepo,
                            IndicatorSeriesRepository seriesRepo,
                            RegionRepository regionRepo) {
        this.indicatorRepo = indicatorRepo;
        this.seriesRepo = seriesRepo;
        this.regionRepo = regionRepo;
    }

    @GetMapping("/indicators")
    public ResponseEntity<?> exportIndicators(@RequestParam String code,
                                               @RequestParam(defaultValue = "json") String format) {
        indicatorRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Indicateur non trouvé: " + code));

        List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeOrderByYearAsc(code);

        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder csv = new StringBuilder("indicator_code,region_id,year,value,national\n");
            for (IndicatorSeries s : series) {
                csv.append(s.getIndicatorCode()).append(",")
                        .append(s.getRegionId() != null ? s.getRegionId() : "").append(",")
                        .append(s.getYear()).append(",")
                        .append(s.getValue()).append(",")
                        .append(s.getNational()).append("\n");
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + code + ".csv")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(csv.toString());
        }

        return ResponseEntity.ok(series.stream()
                .map(s -> Map.of("year", s.getYear(), "value", s.getValue(),
                        "regionId", s.getRegionId() != null ? s.getRegionId() : "",
                        "national", s.getNational()))
                .toList());
    }

    @GetMapping("/regions/{id}")
    public ResponseEntity<?> exportRegion(@PathVariable String id,
                                           @RequestParam(defaultValue = "json") String format) {
        Region region = regionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Région non trouvée: " + id));

        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder csv = new StringBuilder("id,name,code,chief_town,population,area_km2,density\n");
            csv.append(region.getId()).append(",")
                    .append(region.getName()).append(",")
                    .append(region.getCode()).append(",")
                    .append(region.getChiefTown() != null ? region.getChiefTown() : "").append(",")
                    .append(region.getPopulation() != null ? region.getPopulation() : "").append(",")
                    .append(region.getAreaKm2() != null ? region.getAreaKm2() : "").append(",")
                    .append(region.getPopulationDensity() != null ? region.getPopulationDensity() : "").append("\n");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=region_" + id + ".csv")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(csv.toString());
        }

        return ResponseEntity.ok(region);
    }
}
