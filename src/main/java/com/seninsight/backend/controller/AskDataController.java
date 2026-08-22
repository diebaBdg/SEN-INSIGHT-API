package com.seninsight.backend.controller;

import com.seninsight.backend.entity.AskHistory;
import com.seninsight.backend.entity.Conversation;
import com.seninsight.backend.entity.IndicatorSeries;
import com.seninsight.backend.entity.Region;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.AskHistoryRepository;
import com.seninsight.backend.repository.ConversationRepository;
import com.seninsight.backend.repository.IndicatorSeriesRepository;
import com.seninsight.backend.repository.RegionRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/ask")
@Tag(name = "Copilote IA (AskData)")
public class AskDataController {

    private final AskHistoryRepository askRepo;
    private final ConversationRepository conversationRepo;
    private final IndicatorSeriesRepository seriesRepo;
    private final RegionRepository regionRepo;

    public AskDataController(AskHistoryRepository askRepo,
                             ConversationRepository conversationRepo,
                             IndicatorSeriesRepository seriesRepo,
                             RegionRepository regionRepo) {
        this.askRepo = askRepo;
        this.conversationRepo = conversationRepo;
        this.seriesRepo = seriesRepo;
        this.regionRepo = regionRepo;
    }

    @PostMapping("/query")
    public Map<String, Object> query(@Valid @RequestBody Map<String, String> body, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        String question = body.get("query");

        String insight = generateInsight(question);
        List<Map<String, Object>> dataPoints = extractDataPoints(question);
        List<Map<String, String>> sources = List.of(
                Map.of("code", "RGPH", "name", "Recensement Général de la Population et de l'Habitat", "url", "https://www.ansd.sn"),
                Map.of("code", "ANSD", "name", "Agence Nationale de la Statistique et de la Démographie", "url", "https://www.ansd.sn")
        );

        Conversation conversation = Conversation.builder()
                .userId(userId)
                .title(question.length() > 50 ? question.substring(0, 50) + "..." : question)
                .build();
        conversationRepo.save(conversation);

        AskHistory history = AskHistory.builder()
                .userId(userId)
                .conversationId(conversation.getId())
                .query(question)
                .insight(insight)
                .dataPoints(dataPoints.toString())
                .sources(sources.toString())
                .build();
        askRepo.save(history);

        return Map.of(
                "query", question,
                "insight", insight,
                "dataPoints", dataPoints,
                "sources", sources,
                "conversationId", conversation.getId().toString()
        );
    }

    @PostMapping("/analyze-region")
    public Map<String, Object> analyzeRegion(@Valid @RequestBody Map<String, String> body, Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        String regionId = body.get("regionId");
        Region region = regionRepo.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Région non trouvée: " + regionId));

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("region", region);

        List<Map<String, Object>> indicators = new ArrayList<>();
        for (String code : List.of("POP_TOT", "TAUX_CHOM", "TAUX_ALPH", "TAUX_POVERT", "PIB_REG")) {
            List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeAndRegionIdOrderByYearAsc(code, regionId);
            if (!series.isEmpty()) {
                IndicatorSeries latest = series.get(series.size() - 1);
                Map<String, Object> entry = new HashMap<>();
                entry.put("indicator", code);
                entry.put("latestValue", latest.getValue());
                entry.put("latestYear", latest.getYear());
                entry.put("trend", series.size() > 1 ?
                        latest.getValue() - series.get(0).getValue() : 0.0);
                indicators.add(entry);
            }
        }
        analysis.put("indicators", indicators);
        analysis.put("summary", "Analyse de la région de " + region.getName() + " — " + indicators.size() + " indicateurs disponibles");

        AskHistory history = AskHistory.builder()
                .userId(userId)
                .query("Analyse région: " + region.getName())
                .insight(analysis.get("summary").toString())
                .regionId(regionId)
                .build();
        askRepo.save(history);

        return analysis;
    }

    @GetMapping("/suggestions")
    public List<String> getSuggestions() {
        return List.of(
                "Quelle est la population de la région de Dakar ?",
                "Compare le taux de chômage entre Dakar et Thiès",
                "Quel est le taux d'alphabétisation au Sénégal ?",
                "Quelle région a le plus haut PIB régional ?",
                "Comment a évolué le taux de pauvreté à Kédougou ?",
                "Quelles sont les régions les plus densément peuplées ?"
        );
    }

    @GetMapping("/history")
    public Page<AskHistory> getHistory(@RequestParam(required = false) UUID userId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        Authentication auth) {
        UUID uid = userId != null ? userId : (UUID) auth.getPrincipal();
        Pageable pageable = PageRequest.of(page, size);
        return askRepo.findByUserIdOrderByCreatedAtDesc(uid, pageable);
    }

    @GetMapping("/conversations")
    public List<Conversation> getConversations(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return conversationRepo.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    private String generateInsight(String question) {
        String q = question.toLowerCase();
        if (q.contains("population")) {
            return "Dakar est la région la plus peuplée du Sénégal avec environ 3,8 millions d'habitants (RGPH 2023). La densité y atteint 6 978 hab/km².";
        } else if (q.contains("chômage") || q.contains("chomage")) {
            return "Le taux de chômage national est de 13,6% (ENES 2021). Dakar a le taux le plus élevé (17,8%), tandis que Diourbel est parmi les plus faibles (8,4%).";
        } else if (q.contains("alphabétisation") || q.contains("alphabetisation")) {
            return "Le taux d'alphabétisation national a progressé de 51,2% en 2019 à 57,5% en 2023. Dakar reste en tête avec 78,2%.";
        } else if (q.contains("pauvreté") || q.contains("pauvrete")) {
            return "Le taux de pauvreté national est passé de 37,8% (2019) à 34,8% (2023). Kédougou reste la région la plus touchée (63,3%).";
        } else if (q.contains("pib")) {
            return "Dakar concentre environ 50% du PIB national avec 10 500 milliards FCFA en 2023. Thiès suit avec 2 600 milliards FCFA.";
        }
        return "Question analysée. Les données du Sénégal montrent des disparités régionales significatives sur les indicateurs socio-économiques.";
    }

    private List<Map<String, Object>> extractDataPoints(String question) {
        List<Map<String, Object>> points = new ArrayList<>();
        String q = question.toLowerCase();

        if (q.contains("dakar") || q.contains("population")) {
            List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeAndRegionIdOrderByYearAsc("POP_TOT", "dakar");
            series.forEach(s -> points.add(Map.of(
                    "indicator", "POP_TOT",
                    "region", "dakar",
                    "year", s.getYear(),
                    "value", s.getValue()
            )));
        }
        if (q.contains("chômage") || q.contains("chomage") || q.contains("emploi")) {
            List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeAndNationalTrueOrderByYearAsc("TAUX_CHOM");
            series.forEach(s -> points.add(Map.of(
                    "indicator", "TAUX_CHOM",
                    "region", "national",
                    "year", s.getYear(),
                    "value", s.getValue()
            )));
        }
        if (points.isEmpty()) {
            List<IndicatorSeries> series = seriesRepo.findByIndicatorCodeAndNationalTrueOrderByYearAsc("POP_TOT");
            series.forEach(s -> points.add(Map.of(
                    "indicator", "POP_TOT",
                    "region", "national",
                    "year", s.getYear(),
                    "value", s.getValue()
            )));
        }
        return points;
    }
}
