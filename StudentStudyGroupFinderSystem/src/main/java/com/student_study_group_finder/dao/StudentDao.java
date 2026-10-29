package com.student_study_group_finder.dao;

import com.student_study_group_finder.model.Student;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for the students table. Every query uses PreparedStatement.
 */
public class StudentDao {

    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM students WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Saves a new student. Account status is always PENDING by default. */
    public boolean register(String fullName, String email, String password,
                            String course, Integer yearLevel, String role,
                            String dateRegistered, String timeRegistered) {
        String sql = """
                INSERT INTO students
                    (full_name, email, password_hash, course, year_level, role,
                     date_registered, time_registered, account_status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, hashPassword(password));
            if (course == null) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, course);
            }
            if (yearLevel == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, yearLevel);
            }
            ps.setString(6, role);
            ps.setString(7, dateRegistered);
            ps.setString(8, timeRegistered);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    /** Returns the student if email and password match, otherwise empty. */
    public Optional<Student> login(String email, String password) {
        String sql = """
                SELECT student_id, full_name, email, role, password_hash, course,
                       year_level, date_registered, time_registered, account_status
                FROM students WHERE email = ? AND password_hash = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hashPassword(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int yearLevelValue = rs.getInt("year_level");
                    Integer yearLevel = rs.wasNull() ? null : yearLevelValue;
                    return Optional.of(new Student(
                            rs.getInt("student_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("password_hash"),
                            rs.getString("course"),
                            yearLevel,
                            rs.getString("date_registered"),
                            rs.getString("time_registered"),
                            rs.getString("account_status")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
        return Optional.empty();
    }

    /** Returns the user id for an email, or -1 when the email is not registered. */
    public int findIdByEmail(String email) {
        String sql = "SELECT student_id FROM students WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("student_id");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return -1;
    }

    /** Returns all registered users regardless of role or status. */
    public List<Student> getAllUsers() {
        String sql = """
                SELECT student_id, full_name, email, role, password_hash, course,
                       year_level, date_registered, time_registered, account_status
                FROM students
                """;
        List<Student> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int yearLevelValue = rs.getInt("year_level");
                Integer yearLevel = rs.wasNull() ? null : yearLevelValue;
                users.add(new Student(
                        rs.getInt("student_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("password_hash"),
                        rs.getString("course"),
                        yearLevel,
                        rs.getString("date_registered"),
                        rs.getString("time_registered"),
                        rs.getString("account_status")));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return users;
    }

    /** Returns all users where account_status = 'PENDING'. */
    public List<Student> getPendingUsers() {
        String sql = """
                SELECT student_id, full_name, email, role, password_hash, course,
                       year_level, date_registered, time_registered, account_status
                FROM students WHERE account_status = ?
                """;
        List<Student> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "PENDING");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int yearLevelValue = rs.getInt("year_level");
                    Integer yearLevel = rs.wasNull() ? null : yearLevelValue;
                    users.add(new Student(
                            rs.getInt("student_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("password_hash"),
                            rs.getString("course"),
                            yearLevel,
                            rs.getString("date_registered"),
                            rs.getString("time_registered"),
                            rs.getString("account_status")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return users;
    }

    /** Approves a user account by updating status from 'PENDING' to 'APPROVED'. */
    public boolean approveUser(int userId) {
        String sql = "UPDATE students SET account_status = 'APPROVED' WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            return rows == 1;
        } catch (SQLException e) {
            System.out.println("Approval failed: " + e.getMessage());
            return false;
        }
    }

    /** Deactivates a user account by updating status to 'DEACTIVATED'. */
    public boolean deactivateAccount(int userId) {
        String sql = "UPDATE students SET account_status = 'DEACTIVATED' WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();
            return rows == 1;
        } catch (SQLException e) {
            System.out.println("Deactivation failed: " + e.getMessage());
            return false;
        }
    }

    /** Updates a student's profile information. */
    public boolean updateProfile(int userId, String fullName, String course, Integer yearLevel) {
        String sql = "UPDATE students SET full_name = ?, course = ?, year_level = ? WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fullName);
            if (course == null || course.isEmpty()) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, course);
            }
            if (yearLevel == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, yearLevel);
            }
            ps.setInt(4, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Profile update failed: " + e.getMessage());
            return false;
        }
    }

    /** Returns true when the given password matches the stored hash. */
    public boolean checkPassword(int userId, String password) {
        String sql = "SELECT 1 FROM students WHERE student_id = ? AND password_hash = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, hashPassword(password));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Replaces the stored password hash with a new one. */
    public boolean updatePassword(int userId, String newPassword) {
        String sql = "UPDATE students SET password_hash = ? WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(newPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Password change failed: " + e.getMessage());
            return false;
        }
    }

    /** Updates a user's role and account status. Inputs are uppercased before saving. */
    public boolean updateUserRoleAndStatus(int userId, String newRole, String newStatus) {
        String sql = "UPDATE students SET role = ?, account_status = ? WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newRole == null ? null : newRole.toUpperCase());
            ps.setString(2, newStatus == null ? null : newStatus.toUpperCase());
            ps.setInt(3, userId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("User update failed: " + e.getMessage());
            return false;
        }
    }

    /** Returns a single user by ID. */
    public Student getUserById(int userId) {
        String sql = """
                SELECT student_id, full_name, email, role, password_hash, course,
                       year_level, date_registered, time_registered, account_status
                FROM students WHERE student_id = ?
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int yearLevelValue = rs.getInt("year_level");
                    Integer yearLevel = rs.wasNull() ? null : yearLevelValue;
                    return new Student(
                            rs.getInt("student_id"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("role"),
                            rs.getString("password_hash"),
                            rs.getString("course"),
                            yearLevel,
                            rs.getString("date_registered"),
                            rs.getString("time_registered"),
                            rs.getString("account_status"));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    /** SHA-256 hex digest stored in the password_hash column. */
    static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
