package com.seninsight.backend.controller;

import com.seninsight.backend.entity.Indicator;
import com.seninsight.backend.entity.IndicatorSeries;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.IndicatorRepository;
import com.seninsight.backend.repository.IndicatorSeriesRepository;
import com.seninsight.backend.repository.RegionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/indicators")
@Tag(name = "Indicateurs & Séries")
public class IndicatorController {

    private final IndicatorRepository indicatorRepo;
    private final IndicatorSeriesRepository seriesRepo;
    private final RegionRepository regionRepo;

    public IndicatorController(IndicatorRepository indicatorRepo,
                               IndicatorSeriesRepository seriesRepo,
                               RegionRepository regionRepo) {
        this.indicatorRepo = indicatorRepo;
        this.seriesRepo = seriesRepo;
        this.regionRepo = regionRepo;
    }

    @GetMapping
    public List<Indicator> listIndicators() {
        return indicatorRepo.findAllByOrderByCategoryAscNameAsc();
    }

    @GetMapping("/{code}")
    public Indicator getIndicator(@PathVariable String code) {
        return indicatorRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Indicateur non trouvé: " + code));
    }

    @GetMapping("/{code}/series")
    public List<Map<String, Object>> getSeries(@PathVariable String code,
                                                @RequestParam(required = false) String region,
                                                @RequestParam(required = false) Integer from,
                                                @RequestParam(required = false) Integer to) {
        indicatorRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Indicateur non trouvé: " + code));

        List<IndicatorSeries> series;
        if (region != null && from != null && to != null) {
            series = seriesRepo.findByIndicatorCodeAndRegionIdAndYearBetweenOrderByYearAsc(code, region, from, to);
        } else if (region != null) {
            series = seriesRepo.findByIndicatorCodeAndRegionIdOrderByYearAsc(code, region);
        } else {
            series = seriesRepo.findByIndicatorCodeOrderByYearAsc(code);
        }

        return series.stream()
                .map(s -> {
                    Map<String, Object> point = new HashMap<>();
                    point.put("year", s.getYear());
                    point.put("value", s.getValue());
                    point.put("regionId", s.getRegionId());
                    point.put("national", s.getNational());
                    return point;
                })
                .toList();
    }

    @GetMapping("/{code}/ranking")
    public List<Map<String, Object>> getRanking(@PathVariable String code,
                                                  @RequestParam Integer year) {
        indicatorRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Indicateur non trouvé: " + code));

        List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeAndYearAndNationalFalseOrderByValueDesc(code, year);

        return series.stream()
                .map(s -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("regionId", s.getRegionId());
                    entry.put("value", s.getValue());
                    regionRepo.findById(s.getRegionId()).ifPresent(r -> entry.put("regionName", r.getName()));
                    return entry;
                })
                .toList();
    }

    @GetMapping("/{code}/national")
    public List<Map<String, Object>> getNational(@PathVariable String code) {
        indicatorRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Indicateur non trouvé: " + code));

        List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeAndNationalTrueOrderByYearAsc(code);

        return series.stream()
                .map(s -> Map.<String, Object>of("year", s.getYear(), "value", s.getValue()))
                .toList();
    }

    @GetMapping("/{code}/methodology")
    public Map<String, Object> getMethodology(@PathVariable String code) {
        Indicator indicator = indicatorRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Indicateur non trouvé: " + code));
        return Map.of(
                "code", indicator.getCode(),
                "name", indicator.getName(),
                "methodology", indicator.getMethodology() != null ? indicator.getMethodology() : "",
                "unit", indicator.getUnit() != null ? indicator.getUnit() : "",
                "source", indicator.getSourceCode() != null ? indicator.getSourceCode() : ""
        );
    }
}
