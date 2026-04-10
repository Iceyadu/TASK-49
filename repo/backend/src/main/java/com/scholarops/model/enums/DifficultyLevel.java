package com.scholarops.model.enums;

public enum DifficultyLevel {
    VERY_EASY(1),
    EASY(2),
    MEDIUM(3),
    HARD(4),
    VERY_HARD(5);

    private final int level;

    DifficultyLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static DifficultyLevel fromLevel(int level) {
        for (DifficultyLevel dl : values()) {
            if (dl.level == level) return dl;
        }
        throw new IllegalArgumentException("Invalid difficulty level: " + level);
    }
}
