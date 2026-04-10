package com.scholarops.repository;

import com.scholarops.model.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByUserId(Long userId);

    List<Schedule> findByUserIdAndStartTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime")
    List<Schedule> findConflicting(@Param("userId") Long userId,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);
}
