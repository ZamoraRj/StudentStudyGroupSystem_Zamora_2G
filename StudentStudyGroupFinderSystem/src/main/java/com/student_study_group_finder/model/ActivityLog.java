package com.student_study_group_finder.model;

/**
 * Immutable data model for a row in the activity_logs table.
 */
public record ActivityLog(
        int logId,
        int studentId,
        String action,
        String details,
        String logDate,
        String logTime,
        String ipAddress) {
}
