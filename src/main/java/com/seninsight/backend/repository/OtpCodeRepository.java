package com.seninsight.backend.repository;

import com.seninsight.backend.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {
    Optional<OtpCode> findTopByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(String email, String code);
    void deleteByEmailAndUsedFalse(String email);
}
