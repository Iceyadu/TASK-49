package com.scholarops.repository;

import com.scholarops.model.entity.ScheduleChangeJournal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleChangeJournalRepository extends JpaRepository<ScheduleChangeJournal, Long> {

    List<ScheduleChangeJournal> findByUserIdOrderBySequenceNumberDesc(Long userId);

    Optional<ScheduleChangeJournal> findTopByUserIdOrderBySequenceNumberDesc(Long userId);

    List<ScheduleChangeJournal> findByUserIdAndIsUndone(Long userId, Boolean isUndone);

    Optional<ScheduleChangeJournal> findTopByUserIdAndIsUndoneFalseOrderBySequenceNumberDesc(Long userId);

    Optional<ScheduleChangeJournal> findTopByUserIdAndIsUndoneTrueOrderBySequenceNumberDesc(Long userId);

    @Query("SELECT MAX(j.sequenceNumber) FROM ScheduleChangeJournal j WHERE j.user.id = :userId")
    Integer findMaxSequenceNumberByUserId(@Param("userId") Long userId);
}
