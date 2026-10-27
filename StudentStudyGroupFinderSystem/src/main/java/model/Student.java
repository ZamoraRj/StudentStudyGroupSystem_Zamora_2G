package model;

/**
 * Immutable data model for a row in the students table.
 */
public record Student(
        int studentId,
        String fullName,
        String email,
        String role,
        String passwordHash,
        String course,
        Integer yearLevel,
        String dateRegistered,
        String accountStatus,
        String createdAt,
        String updatedAt) {
}
