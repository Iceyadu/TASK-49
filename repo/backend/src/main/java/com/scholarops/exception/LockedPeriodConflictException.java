package com.scholarops.exception;

import java.time.LocalDateTime;

public class LockedPeriodConflictException extends RuntimeException {

    private final Long conflictingPeriodId;
    private final String conflictingPeriodTitle;
    private final LocalDateTime conflictStart;
    private final LocalDateTime conflictEnd;

    public LockedPeriodConflictException(String message, Long conflictingPeriodId,
                                          String conflictingPeriodTitle,
                                          LocalDateTime conflictStart,
                                          LocalDateTime conflictEnd) {
        super(message);
        this.conflictingPeriodId = conflictingPeriodId;
        this.conflictingPeriodTitle = conflictingPeriodTitle;
        this.conflictStart = conflictStart;
        this.conflictEnd = conflictEnd;
    }

    public Long getConflictingPeriodId() {
        return conflictingPeriodId;
    }

    public String getConflictingPeriodTitle() {
        return conflictingPeriodTitle;
    }

    public LocalDateTime getConflictStart() {
        return conflictStart;
    }

    public LocalDateTime getConflictEnd() {
        return conflictEnd;
    }
}
