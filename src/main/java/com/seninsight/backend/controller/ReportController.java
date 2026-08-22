package com.seninsight.backend.controller;

import com.seninsight.backend.entity.Report;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.ReportRepository;
import com.seninsight.backend.repository.RegionRepository;
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
@Tag(name = "Rapports")
public class ReportController {

    private final ReportRepository reportRepo;
    private final RegionRepository regionRepo;

    public ReportController(ReportRepository reportRepo, RegionRepository regionRepo) {
        this.reportRepo = reportRepo;
        this.regionRepo = regionRepo;
    }

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

    @GetMapping
    public Page<Report> listReports(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        return reportRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @GetMapping("/{id}")
    public Report getReport(@PathVariable UUID id, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return reportRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID id,
                                                  @RequestParam(defaultValue = "pdf") String format,
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

    @PostMapping("/{id}/share")
    public Map<String, Object> shareReport(@PathVariable UUID id, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        Report report = reportRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));
        String token = UUID.randomUUID().toString().replace("-", "");
        report.setShareToken(token);
        reportRepo.save(report);
        return Map.of("shareToken", token, "message", "Lien de partage généré");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteReport(@PathVariable UUID id, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        Report report = reportRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Rapport non trouvé"));
        reportRepo.delete(report);
        return Map.of("message", "Rapport supprimé");
    }
}
