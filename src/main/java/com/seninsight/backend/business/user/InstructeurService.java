package com.seninsight.backend.business.user;

import com.seninsight.backend.business.user.dtos.InstructeurStatsDTO;
import com.seninsight.backend.config.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class InstructeurService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public InstructeurStatsDTO getInstructeurStats(UUID instructeurId) {
        log.info("Récupération des statistiques de l'instructeur: {}", instructeurId);

        User instructeur = userRepository.findById(instructeurId)
                .orElseThrow(() -> new BusinessException("INSTRUCTEUR_NOT_FOUND",
                        "Instructeur non trouvé avec l'ID: " + instructeurId, instructeurId));

        if (!instructeur.isInstructeur()) {
            throw new BusinessException("NOT_INSTRUCTEUR",
                    "L'utilisateur n'est pas un instructeur", instructeurId);
        }

        return InstructeurStatsDTO.builder()
                .instructeurId(instructeurId)
                .fullName(instructeur.getFullName())
                .totalAssigned(safeInt(instructeur.getTotalAssigned()))
                .totalApproved(safeInt(instructeur.getTotalApproved()))
                .totalRejected(safeInt(instructeur.getTotalRejected()))
                .tauxApprobation(calculateTauxApprobation(instructeur))
                .performance(calculatePerformance(instructeur))
                .build();
    }

    @Transactional(readOnly = true)
    public List<InstructeurStatsDTO> getAllInstructeursStats() {
        log.info("Récupération des statistiques de tous les instructeurs");

        return userRepository.findAll().stream()
                .filter(User::isInstructeur)
                .map(instructeur -> InstructeurStatsDTO.builder()
                        .instructeurId(instructeur.getId())
                        .fullName(instructeur.getFullName())
                        .totalAssigned(safeInt(instructeur.getTotalAssigned()))
                        .totalApproved(safeInt(instructeur.getTotalApproved()))
                        .totalRejected(safeInt(instructeur.getTotalRejected()))
                        .tauxApprobation(calculateTauxApprobation(instructeur))
                        .performance(calculatePerformance(instructeur))
                        .build())
                .collect(Collectors.toList());
    }

    public void incrementAssigned(UUID instructeurId) {
        log.info("Incrémentation du nombre de demandes assignées pour l'instructeur: {}", instructeurId);
        updateStats(instructeurId, "ASSIGN");
    }

    public void incrementApproved(UUID instructeurId) {
        log.info("Incrémentation du nombre de demandes approuvées pour l'instructeur: {}", instructeurId);
        updateStats(instructeurId, "APPROVE");
    }

    public void incrementRejected(UUID instructeurId) {
        log.info("Incrémentation du nombre de demandes rejetées pour l'instructeur: {}", instructeurId);
        updateStats(instructeurId, "REJECT");
    }

    public void resetStats(UUID instructeurId) {
        log.info("Réinitialisation des statistiques de l'instructeur: {}", instructeurId);
        updateStats(instructeurId, "RESET");
    }

    private void updateStats(UUID instructeurId, String action) {
        User instructeur = userRepository.findById(instructeurId)
                .orElseThrow(() -> new BusinessException("INSTRUCTEUR_NOT_FOUND",
                        "Instructeur non trouvé avec l'ID: " + instructeurId, instructeurId));

        if (!instructeur.isInstructeur()) {
            throw new BusinessException("NOT_INSTRUCTEUR",
                    "L'utilisateur n'est pas un instructeur", instructeurId);
        }

        switch (action.toUpperCase()) {
            case "ASSIGN":
                instructeur.setTotalAssigned(safeInt(instructeur.getTotalAssigned()) + 1);
                break;
            case "APPROVE":
                instructeur.setTotalApproved(safeInt(instructeur.getTotalApproved()) + 1);
                break;
            case "REJECT":
                instructeur.setTotalRejected(safeInt(instructeur.getTotalRejected()) + 1);
                break;
            case "RESET":
                instructeur.setTotalAssigned(0);
                instructeur.setTotalApproved(0);
                instructeur.setTotalRejected(0);
                break;
            default:
                throw new BusinessException("INVALID_ACTION",
                        "Action non valide: " + action, action);
        }

        userRepository.save(instructeur);
        log.info("Statistiques mises à jour avec succès pour l'instructeur: {}", instructeurId);
    }

    private Double calculateTauxApprobation(User instructeur) {
        int assigned = safeInt(instructeur.getTotalAssigned());
        if (assigned == 0) {
            return 0.0;
        }
        int approved = safeInt(instructeur.getTotalApproved());
        return (double) approved / assigned * 100;
    }

    private String calculatePerformance(User instructeur) {
        double taux = calculateTauxApprobation(instructeur);
        if (taux >= 80) return "EXCELLENT";
        if (taux >= 60) return "BON";
        if (taux >= 40) return "MOYEN";
        return "FAIBLE";
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
