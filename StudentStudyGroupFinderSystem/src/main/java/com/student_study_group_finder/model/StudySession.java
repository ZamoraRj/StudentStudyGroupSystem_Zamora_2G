package com.student_study_group_finder.model;

/**
 * Immutable data model for a row in the study_sessions table.
 */
public record StudySession(
        int sessionId,
        int groupId,
        String sessionDate,
        String startTime,
        String endTime,
        String location,
        int createdBy,
        String status) {
}
