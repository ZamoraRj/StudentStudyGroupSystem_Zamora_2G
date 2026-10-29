package com.student_study_group_finder.model;

/**
 * Immutable data model for a row in the group_memberships table.
 */
public record GroupMembership(
        int membershipId,
        int groupId,
        int studentId,
        String role,
        String joinDate,
        String joinTime,
        String status) {
}
