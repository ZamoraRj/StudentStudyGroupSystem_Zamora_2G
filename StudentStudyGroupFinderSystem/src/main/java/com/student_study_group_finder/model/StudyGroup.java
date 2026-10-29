package com.student_study_group_finder.model;

/**
 * Immutable data model for a row in the study_groups table.
 */
public record StudyGroup(
        int groupId,
        String groupName,
        String description,
        String subject,
        String schedule,
        int maxMembers,
        int createdBy,
        String status) {
}
