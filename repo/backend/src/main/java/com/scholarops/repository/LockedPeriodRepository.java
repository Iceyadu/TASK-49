package com.scholarops.repository;

import com.scholarops.model.entity.LockedPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LockedPeriodRepository extends JpaRepository<LockedPeriod, Long> {

    List<LockedPeriod> findByUserId(Long userId);

    @Query("SELECT lp FROM LockedPeriod lp WHERE lp.user.id = :userId " +
           "AND lp.startTime < :endTime AND lp.endTime > :startTime")
    List<LockedPeriod> findConflicting(@Param("userId") Long userId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);
}
