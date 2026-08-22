package com.seninsight.backend.repository;

import com.seninsight.backend.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceRepository extends JpaRepository<Source, UUID> {
    Optional<Source> findByCode(String code);
    List<Source> findAllByOrderByNameAsc();
}
