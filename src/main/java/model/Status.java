package model;

/**
 * Represents the user's current relationship with a manga.
 *
 * These values are used to organize manga into different tracker sections,
 * such as plan-to-read, currently reading, completed, and dropped.
 */

public enum Status {
    READING,
    COMPLETED,
    DROPPED,
    PLAN_TO_READ
}
