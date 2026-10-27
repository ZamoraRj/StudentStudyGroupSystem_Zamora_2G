package dao;

import model.Student;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
                            String dateRegistered, String createdAt, String updatedAt) {
        String sql = """
                INSERT INTO students
                    (full_name, email, password_hash, course, year_level, role,
                     date_registered, account_status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
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
            ps.setString(8, createdAt);
            ps.setString(9, updatedAt);
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
                       year_level, date_registered, account_status,
                       created_at, updated_at
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
                            rs.getString("account_status"),
                            rs.getString("created_at"),
                            rs.getString("updated_at")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }
        return Optional.empty();
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
