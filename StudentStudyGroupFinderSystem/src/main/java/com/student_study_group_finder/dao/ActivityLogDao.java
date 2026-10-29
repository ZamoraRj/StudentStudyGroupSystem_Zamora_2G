package com.student_study_group_finder.dao;

import com.student_study_group_finder.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the activity_logs table. Every query uses PreparedStatement.
 */
public class ActivityLogDao {

    /** Logs a new activity entry, capturing the caller's IP address automatically. */
    public boolean logActivity(int studentId, String action,
                                String details, String logDate, String logTime) {
        return logActivity(studentId, action, details, logDate, logTime, ClientInfo.localIpAddress());
    }

    /** Logs a new activity entry with an explicit IP address. */
    public boolean logActivity(int studentId, String action,
                                String details, String logDate, String logTime, String ipAddress) {
        String sql = """
                INSERT INTO activity_logs (student_id, action, details, log_date, log_time, ip_address)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, action);
            if (details == null || details.isEmpty()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, details);
            }
            ps.setString(4, logDate);
            ps.setString(5, logTime);
            if (ipAddress == null || ipAddress.isEmpty()) {
                ps.setNull(6, Types.VARCHAR);
            } else {
                ps.setString(6, ipAddress);
            }
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to log activity: " + e.getMessage());
            return false;
        }
    }

    /** Returns all activity logs ordered by date/time descending. */
    public List<ActivityLog> getAllLogs() {
        String sql = "SELECT log_id, student_id, action, details, log_date, log_time, ip_address " +
                     "FROM activity_logs ORDER BY log_date DESC, log_time DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                logs.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return logs;
    }

    /** Returns activity logs for a specific user. */
    public List<ActivityLog> getLogsByStudent(int studentId) {
        String sql = "SELECT log_id, student_id, action, details, log_date, log_time, ip_address " +
                     "FROM activity_logs WHERE student_id = ? ORDER BY log_date DESC, log_time DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return logs;
    }

    /** Returns recent activity logs (limited count). */
    public List<ActivityLog> getRecentLogs(int limit) {
        String sql = "SELECT log_id, student_id, action, details, log_date, log_time, ip_address " +
                     "FROM activity_logs ORDER BY log_date DESC, log_time DESC LIMIT ?";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return logs;
    }

    /** Returns count of logs grouped by action type for summary reports. */
    public List<ActionCount> getActionSummary() {
        String sql = "SELECT action, COUNT(*) AS count FROM activity_logs GROUP BY action ORDER BY count DESC";
        List<ActionCount> summary = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                summary.add(new ActionCount(rs.getString("action"), rs.getInt("count")));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return summary;
    }

    /** Deletes all activity logs (admin use). */
    public boolean clearAllLogs() {
        String sql = "DELETE FROM activity_logs";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            System.out.println("Failed to clear logs: " + e.getMessage());
            return false;
        }
    }

    private static String readIpAddress(ResultSet rs) {
        try {
            return rs.getString("ip_address");
        } catch (SQLException e) {
            return null;
        }
    }

    private ActivityLog mapResultSet(ResultSet rs) throws SQLException {
        return new ActivityLog(
                rs.getInt("log_id"),
                rs.getInt("student_id"),
                rs.getString("action"),
                rs.getString("details"),
                rs.getString("log_date"),
                rs.getString("log_time"),
                readIpAddress(rs));
    }

    /** Simple record for action summary counts. */
    public record ActionCount(String action, int count) {
    }
}
