package com.seninsight.backend.repository;

import com.seninsight.backend.entity.IndicatorSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IndicatorSeriesRepository extends JpaRepository<IndicatorSeries, UUID> {
    List<IndicatorSeries> findByIndicatorCodeAndRegionIdOrderByYearAsc(String indicatorCode, String regionId);
    List<IndicatorSeries> findByIndicatorCodeAndNationalTrueOrderByYearAsc(String indicatorCode);
    List<IndicatorSeries> findByIndicatorCodeAndYearAndNationalFalseOrderByValueDesc(String indicatorCode, Integer year);
    List<IndicatorSeries> findByIndicatorCodeAndRegionIdAndYearBetweenOrderByYearAsc(String indicatorCode, String regionId, Integer fromYear, Integer toYear);
    List<IndicatorSeries> findByIndicatorCodeOrderByYearAsc(String indicatorCode);
}
