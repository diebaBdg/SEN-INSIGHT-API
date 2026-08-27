package com.seninsight.backend.controller;

import com.seninsight.backend.entity.Report;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.ReportRepository;
import com.seninsight.backend.repository.RegionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/reports")
@Tag(name = "Rapports", description = "Générez, consultez, téléchargez et partagez des rapports analytiques")
public class ReportController {

    private final ReportRepository reportRepo;
    private final RegionRepository regionRepo;

    public ReportController(ReportRepository reportRepo, RegionRepository regionRepo) {
        this.reportRepo = reportRepo;
        this.regionRepo = regionRepo;
    }

    @Operation(summary = "Générer un rapport", description = "Crée un rapport analytique pour une région et une période données, incluant les indicateurs sélectionnés.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rapport généré avec succès",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Report.class))),
            @ApiResponse(responseCode = "400", description = "Requête invalide", content = @Content)
    })
    @PostMapping("/generate")
    public Report generateReport(@Valid @RequestBody Map<String, Object> body, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        String regionId = (String) body.get("regionId");
        @SuppressWarnings("unchecked")
        List<String> indicatorCodes = (List<String>) body.get("indicatorCodes");
        Integer periodStart = (Integer) body.get("periodStart");
        Integer periodEnd = (Integer) body.get("periodEnd");

        String regionName = regionId != null
                ? regionRepo.findById(regionId).map(r -> r.getName()).orElse("Sénégal")
                : "Sénégal";

        String title = "Rapport — " + regionName + " (" + periodStart + "-" + periodEnd + ")";

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("region", regionName);
        content.put("indicators", indicatorCodes);
        content.put("period", Map.of("start", periodStart, "end", periodEnd));
        content.put("generatedAt", Instant.now().toString());
        content.put("summary", "Rapport analytique de la région de " + regionName + " couvrant la période " + periodStart + "-" + periodEnd + ".");

        Report report = Report.builder()
                .userId(userId)
                .title(title)
                .regionId(regionId)
                .indicatorCodes(indicatorCodes != null ? String.join(",", indicatorCodes) : "")
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .content(content.toString())
                .format("pdf")
                .status("generated")
                .build();
        reportRepo.save(report);
        return report;
    }

    @Operation(summary = "Lister les rapports", description = "Retourne les rapports de l'utilisateur connecté, paginés et triés par date de création décroissante.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste paginée des rapports",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Page.class)))
    })
    @GetMapping
    public Page<Report> listReports(
            @Parameter(description = "Numéro de page (0-indexé)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de la page") @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        return reportRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Operation(summary = "Obtenir un rapport par ID", description = "Retourne les détails d'un rapport appartenant à l'utilisateur connecté.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rapport trouvé",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Report.class))),
            @ApiResponse(responseCode = "404", description = "Rapport non trouvé", content = @Content)
    })
    @GetMapping("/{id}")
    public Report getReport(
            @Parameter(description = "Identifiant du rapport", required = true) @PathVariable UUID id, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return reportRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));
    }

    @Operation(summary = "Télécharger un rapport", description = "Télécharge un rapport au format PDF ou CSV.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fichier téléchargé",
                    content = @Content(mediaType = "application/octet-stream")),
            @ApiResponse(responseCode = "404", description = "Rapport non trouvé", content = @Content)
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReport(
            @Parameter(description = "Identifiant du rapport", required = true) @PathVariable UUID id,
            @Parameter(description = "Format de téléchargement : pdf ou csv", example = "pdf") @RequestParam(defaultValue = "pdf") String format,
            Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        Report report = reportRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));

        String content = "SenInsight Report\n================\n\n"
                + "Title: " + report.getTitle() + "\n"
                + "Region: " + report.getRegionId() + "\n"
                + "Period: " + report.getPeriodStart() + " - " + report.getPeriodEnd() + "\n"
                + "Status: " + report.getStatus() + "\n"
                + "Content: " + report.getContent() + "\n";

        byte[] data;
        MediaType mediaType;
        String filename;

        if ("csv".equalsIgnoreCase(format)) {
            data = content.getBytes();
            mediaType = MediaType.TEXT_PLAIN;
            filename = "report_" + id + ".csv";
        } else {
            data = content.getBytes();
            mediaType = MediaType.APPLICATION_PDF;
            filename = "report_" + id + ".pdf";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(mediaType)
                .body(data);
    }

    @Operation(summary = "Partager un rapport", description = "Génère un token de partage public pour un rapport.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lien de partage généré",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Rapport non trouvé", content = @Content)
    })
    @PostMapping("/{id}/share")
    public Map<String, Object> shareReport(
            @Parameter(description = "Identifiant du rapport", required = true) @PathVariable UUID id, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        Report report = reportRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));
        String token = UUID.randomUUID().toString().replace("-", "");
        report.setShareToken(token);
        reportRepo.save(report);
        return Map.of("shareToken", token, "message", "Lien de partage généré");
    }

    @Operation(summary = "Supprimer un rapport", description = "Supprime définitivement un rapport appartenant à l'utilisateur connecté.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rapport supprimé",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "404", description = "Rapport non trouvé", content = @Content)
    })
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteReport(
            @Parameter(description = "Identifiant du rapport", required = true) @PathVariable UUID id, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        Report report = reportRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));
        reportRepo.delete(report);
        return Map.of("message", "Rapport supprimé");
    }
}
