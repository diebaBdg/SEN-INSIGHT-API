package com.seninsight.backend.controller;

import com.seninsight.backend.entity.Source;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.SourceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sources")
@Tag(name = "Sources & Traçabilité", description = "Consultez les sources de données officielles utilisées par la plateforme")
public class SourceController {

    private final SourceRepository sourceRepo;

    public SourceController(SourceRepository sourceRepo) {
        this.sourceRepo = sourceRepo;
    }

    @Operation(summary = "Lister toutes les sources", description = "Retourne toutes les sources de données triées par nom.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des sources",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Source.class)))
    })
    @GetMapping
    public List<Source> listSources() {
        return sourceRepo.findAllByOrderByNameAsc();
    }

    @Operation(summary = "Obtenir une source par code", description = "Retourne les informations d'une source à partir de son code.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Source trouvée",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Source.class))),
            @ApiResponse(responseCode = "404", description = "Source non trouvée", content = @Content)
    })
    @GetMapping("/{code}")
    public Source getSource(
            @Parameter(description = "Code de la source", required = true) @PathVariable String code) {
        return sourceRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Source non trouvée: " + code));
    }
}
