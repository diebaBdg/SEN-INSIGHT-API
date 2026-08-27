package com.seninsight.backend.controller;

import com.seninsight.backend.entity.Indicator;
import com.seninsight.backend.entity.IndicatorSeries;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.IndicatorRepository;
import com.seninsight.backend.repository.IndicatorSeriesRepository;
import com.seninsight.backend.repository.RegionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/indicators")
@Tag(name = "Indicateurs & Séries", description = "Consultez les indicateurs socio-économiques et leurs séries temporelles")
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

    @Operation(summary = "Lister tous les indicateurs", description = "Retourne tous les indicateurs triés par catégorie puis par nom.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des indicateurs",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Indicator.class)))
    })
    @GetMapping
    public List<Indicator> listIndicators() {
        return indicatorRepo.findAllByOrderByCategoryAscNameAsc();
    }

    @Operation(summary = "Obtenir un indicateur par code", description = "Retourne les métadonnées d'un indicateur à partir de son code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Indicateur trouvé",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Indicator.class))),
            @ApiResponse(responseCode = "404", description = "Indicateur non trouvé", content = @Content)
    })
    @GetMapping("/{code}")
    public Indicator getIndicator(
            @Parameter(description = "Code de l'indicateur", required = true) @PathVariable String code) {
        return indicatorRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Indicateur non trouvé: " + code));
    }

    @Operation(summary = "Séries temporelles d'un indicateur", description = "Retourne les séries d'un indicateur, filtrables par région et par période.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Séries récupérées",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Indicateur non trouvé", content = @Content)
    })
    @GetMapping("/{code}/series")
    public List<Map<String, Object>> getSeries(
            @Parameter(description = "Code de l'indicateur", required = true) @PathVariable String code,
            @Parameter(description = "Identifiant de la région (optionnel)") @RequestParam(required = false) String region,
            @Parameter(description = "Année de début (incluse)") @RequestParam(required = false) Integer from,
            @Parameter(description = "Année de fin (incluse)") @RequestParam(required = false) Integer to) {
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

    @Operation(summary = "Classement des régions pour un indicateur", description = "Retourne le classement des régions pour un indicateur et une année donnés, trié par valeur décroissante.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Classement récupéré",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Indicateur non trouvé", content = @Content)
    })
    @GetMapping("/{code}/ranking")
    public List<Map<String, Object>> getRanking(
            @Parameter(description = "Code de l'indicateur", required = true) @PathVariable String code,
            @Parameter(description = "Année du classement", required = true) @RequestParam Integer year) {
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

    @Operation(summary = "Séries nationales d'un indicateur", description = "Retourne les séries nationales (non régionales) d'un indicateur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Séries nationales récupérées",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Indicateur non trouvé", content = @Content)
    })
    @GetMapping("/{code}/national")
    public List<Map<String, Object>> getNational(
            @Parameter(description = "Code de l'indicateur", required = true) @PathVariable String code) {
        indicatorRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Indicateur non trouvé: " + code));

        List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeAndNationalTrueOrderByYearAsc(code);

        return series.stream()
                .map(s -> Map.<String, Object>of("year", s.getYear(), "value", s.getValue()))
                .toList();
    }

    @Operation(summary = "Méthodologie d'un indicateur", description = "Retourne la méthodologie, l'unité et la source d'un indicateur.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Méthodologie récupérée",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Indicateur non trouvé", content = @Content)
    })
    @GetMapping("/{code}/methodology")
    public Map<String, Object> getMethodology(
            @Parameter(description = "Code de l'indicateur", required = true) @PathVariable String code) {
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
