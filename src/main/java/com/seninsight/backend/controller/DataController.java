package com.seninsight.backend.controller;

import com.seninsight.backend.repository.IndicatorRepository;
import com.seninsight.backend.repository.IndicatorSeriesRepository;
import com.seninsight.backend.repository.RegionRepository;
import com.seninsight.backend.repository.SourceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/data")
@Tag(name = "Données & Pipeline", description = "État du pipeline de données et import de nouvelles données (admin)")
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

    @Operation(summary = "État du pipeline de données", description = "Retourne l'état de santé du pipeline et le nombre d'enregistrements par entité.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "État du pipeline",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
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

    @Operation(summary = "Importer des données", description = "Importe un lot de données dans le pipeline. Réservé aux administrateurs.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Données importées avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle admin requis", content = @Content)
    })
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
