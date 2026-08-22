package com.seninsight.backend.controller;

import com.seninsight.backend.entity.Source;
import com.seninsight.backend.exception.ResourceNotFoundException;
import com.seninsight.backend.repository.SourceRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sources")
@Tag(name = "Sources & Traçabilité")
public class SourceController {

    private final SourceRepository sourceRepo;

    public SourceController(SourceRepository sourceRepo) {
        this.sourceRepo = sourceRepo;
    }

    @GetMapping
    public List<Source> listSources() {
        return sourceRepo.findAllByOrderByNameAsc();
    }

    @GetMapping("/{code}")
    public Source getSource(@PathVariable String code) {
        return sourceRepo.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Source non trouvée: " + code));
    }
}
