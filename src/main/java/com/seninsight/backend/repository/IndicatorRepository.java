package com.seninsight.backend.repository;

import com.seninsight.backend.entity.Indicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndicatorRepository extends JpaRepository<Indicator, UUID> {
    Optional<Indicator> findByCode(String code);
    List<Indicator> findAllByOrderByCategoryAscNameAsc();
}
