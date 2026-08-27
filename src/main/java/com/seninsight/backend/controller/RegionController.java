package com.seninsight.backend.controller;

import com.seninsight.backend.entity.IndicatorSeries;
import com.seninsight.backend.entity.Region;
import com.seninsight.backend.exception.ResourceNotFoundException;
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
@RequestMapping("/regions")
@Tag(name = "Régions & Territoires", description = "Consultez les régions du Sénégal, leurs profils et comparez-les entre elles")
public class RegionController {

    private final RegionRepository regionRepo;
    private final IndicatorSeriesRepository seriesRepo;

    public RegionController(RegionRepository regionRepo, IndicatorSeriesRepository seriesRepo) {
        this.regionRepo = regionRepo;
        this.seriesRepo = seriesRepo;
    }

    @Operation(summary = "Lister toutes les régions", description = "Retourne toutes les régions du Sénégal triées par nom.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des régions",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Region.class)))
    })
    @GetMapping
    public List<Region> listRegions() {
        return regionRepo.findAllByOrderByNameAsc();
    }

    @Operation(summary = "Obtenir une région par identifiant", description = "Retourne les informations d'une région à partir de son identifiant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Région trouvée",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Region.class))),
            @ApiResponse(responseCode = "404", description = "Région non trouvée", content = @Content)
    })
    @GetMapping("/{id}")
    public Region getRegion(
            @Parameter(description = "Identifiant de la région", required = true) @PathVariable String id) {
        return regionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Région non trouvée: " + id));
    }

    @Operation(summary = "Profil détaillé d'une région", description = "Retourne le profil complet d'une région : démographie, géographie et séries de population.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profil de la région",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Région non trouvée", content = @Content)
    })
    @GetMapping("/{id}/profile")
    public Map<String, Object> getRegionProfile(
            @Parameter(description = "Identifiant de la région", required = true) @PathVariable String id) {
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

    @Operation(summary = "Comparer plusieurs régions", description = "Compare la population et le taux de pauvreté de plusieurs régions. Les identifiants sont séparés par des virgules.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comparaison générée",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    @GetMapping("/compare")
    public List<Map<String, Object>> compareRegions(
            @Parameter(description = "Identifiants des régions séparés par des virgules", required = true, example = "dakar,thies,saint-louis")
            @RequestParam String ids) {
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

    @Operation(summary = "Indicateurs d'une région", description = "Retourne les séries des indicateurs clés disponibles pour une région donnée.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Indicateurs de la région",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Région non trouvée", content = @Content)
    })
    @GetMapping("/{id}/indicators")
    public List<Map<String, Object>> getRegionIndicators(
            @Parameter(description = "Identifiant de la région", required = true) @PathVariable String id) {
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
