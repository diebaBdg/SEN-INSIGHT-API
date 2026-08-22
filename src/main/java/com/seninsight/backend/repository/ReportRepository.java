package com.seninsight.backend.repository;

import com.seninsight.backend.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {
    Page<Report> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    Optional<Report> findByIdAndUserId(UUID id, UUID userId);
    Optional<Report> findByShareToken(String shareToken);
}
