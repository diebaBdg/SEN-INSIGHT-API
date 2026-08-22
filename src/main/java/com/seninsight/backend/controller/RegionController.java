package com.seninsight.backend.controller;

import com.seninsight.backend.entity.IndicatorSeries;
import com.seninsight.backend.entity.Region;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.IndicatorSeriesRepository;
import com.seninsight.backend.repository.RegionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/regions")
@Tag(name = "Régions & Territoires")
public class RegionController {

    private final RegionRepository regionRepo;
    private final IndicatorSeriesRepository seriesRepo;

    public RegionController(RegionRepository regionRepo, IndicatorSeriesRepository seriesRepo) {
        this.regionRepo = regionRepo;
        this.seriesRepo = seriesRepo;
    }

    @GetMapping
    public List<Region> listRegions() {
        return regionRepo.findAllByOrderByNameAsc();
    }

    @GetMapping("/{id}")
    public Region getRegion(@PathVariable String id) {
        return regionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Région non trouvée: " + id));
    }

    @GetMapping("/{id}/profile")
    public Map<String, Object> getRegionProfile(@PathVariable String id) {
        Region region = regionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Région non trouvée: " + id));

        Map<String, Object> profile = new HashMap<>();
        profile.put("region", region);
        profile.put("demography", Map.of(
                "population", region.getPopulation(),
                "area_km2", region.getAreaKm2(),
                "density", region.getPopulationDensity()
        ));
        profile.put("geography", Map.of(
                "chiefTown", region.getChiefTown(),
                "latitude", region.getLatitude(),
                "longitude", region.getLongitude()
        ));

        List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeOrderByYearAsc("POP_TOT");
        List<Map<String, Object>> popSeries = series.stream()
                .filter(s -> id.equals(s.getRegionId()))
                .map(s -> Map.<String, Object>of("year", s.getYear(), "value", s.getValue()))
                .toList();
        profile.put("populationSeries", popSeries);

        return profile;
    }

    @GetMapping("/compare")
    public List<Map<String, Object>> compareRegions(@RequestParam String ids) {
        List<String> regionIds = List.of(ids.split(","));
        return regionIds.stream().map(rid -> {
            Region region = regionRepo.findById(rid.trim()).orElse(null);
            if (region == null) return Map.<String, Object>of("regionId", rid, "error", "non trouvée");

            Map<String, Object> data = new HashMap<>();
            data.put("region", region);

            List<IndicatorSeries> popSeries = seriesRepo.findByIndicatorCodeAndRegionIdOrderByYearAsc("POP_TOT", rid.trim());
            data.put("population", popSeries.stream()
                    .map(s -> Map.<String, Object>of("year", s.getYear(), "value", s.getValue()))
                    .toList());

            List<IndicatorSeries> povertySeries = seriesRepo.findByIndicatorCodeAndRegionIdOrderByYearAsc("TAUX_POVERT", rid.trim());
            data.put("povertyRate", povertySeries.stream()
                    .map(s -> Map.<String, Object>of("year", s.getYear(), "value", s.getValue()))
                    .toList());

            return data;
        }).toList();
    }

    @GetMapping("/{id}/indicators")
    public List<Map<String, Object>> getRegionIndicators(@PathVariable String id) {
        regionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Région non trouvée: " + id));

        return List.of("POP_TOT", "TAUX_CHOM", "TAUX_ALPH", "TAUX_POVERT", "PIB_REG").stream()
                .map(code -> {
                    List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeAndRegionIdOrderByYearAsc(code, id);
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("indicatorCode", code);
                    entry.put("series", series.stream()
                            .map(s -> Map.<String, Object>of("year", s.getYear(), "value", s.getValue()))
                            .toList());
                    return entry;
                })
                .filter(e -> !((List<?>) e.get("series")).isEmpty())
                .toList();
    }
}
