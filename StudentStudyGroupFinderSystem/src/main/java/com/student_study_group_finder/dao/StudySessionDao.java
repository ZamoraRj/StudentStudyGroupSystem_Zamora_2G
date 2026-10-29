package com.student_study_group_finder.dao;

import com.student_study_group_finder.model.StudySession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the study_sessions table. Every query uses PreparedStatement.
 */
public class StudySessionDao {

    /** Creates a new study session. */
    public boolean createSession(int groupId, String sessionDate, String startTime, String endTime,
                                  String location, int createdBy) {
        String sql = """
                INSERT INTO study_sessions (group_id, session_date, start_time, end_time,
                                            location, created_by, status)
                VALUES (?, ?, ?, ?, ?, ?, 'SCHEDULED')
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setString(2, sessionDate);
            ps.setString(3, startTime);
            ps.setString(4, endTime);
            if (location == null || location.isEmpty()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, location);
            }
            ps.setInt(6, createdBy);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to create session: " + e.getMessage());
            return false;
        }
    }

    /** Returns all sessions for a specific group. */
    public List<StudySession> getSessionsByGroup(int groupId) {
        String sql = "SELECT session_id, group_id, session_date, start_time, end_time, " +
                     "location, created_by, status FROM study_sessions WHERE group_id = ? ORDER BY session_date DESC, start_time DESC";
        List<StudySession> sessions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return sessions;
    }

    /** Returns all sessions across all groups. */
    public List<StudySession> getAllSessions() {
        String sql = "SELECT session_id, group_id, session_date, start_time, end_time, " +
                     "location, created_by, status FROM study_sessions ORDER BY session_date DESC, start_time DESC";
        List<StudySession> sessions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sessions.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return sessions;
    }

    /** Returns a single session by ID. */
    public StudySession getSessionById(int sessionId) {
        String sql = "SELECT session_id, group_id, session_date, start_time, end_time, " +
                     "location, created_by, status FROM study_sessions WHERE session_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return null;
    }

    /** Updates session information. */
    public boolean updateSession(int sessionId, String sessionDate, String startTime, String endTime, String location) {
        String sql = "UPDATE study_sessions SET session_date = ?, start_time = ?, end_time = ?, location = ? WHERE session_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionDate);
            ps.setString(2, startTime);
            ps.setString(3, endTime);
            if (location == null || location.isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, location);
            }
            ps.setInt(5, sessionId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to update session: " + e.getMessage());
            return false;
        }
    }

    /** Updates session status (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED). */
    public boolean updateSessionStatus(int sessionId, String status) {
        String sql = "UPDATE study_sessions SET status = ? WHERE session_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, sessionId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to update session status: " + e.getMessage());
            return false;
        }
    }

    /** Deletes a study session. */
    public boolean deleteSession(int sessionId) {
        String sql = "DELETE FROM study_sessions WHERE session_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to delete session: " + e.getMessage());
            return false;
        }
    }

    private StudySession mapResultSet(ResultSet rs) throws SQLException {
        return new StudySession(
                rs.getInt("session_id"),
                rs.getInt("group_id"),
                rs.getString("session_date"),
                rs.getString("start_time"),
                rs.getString("end_time"),
                rs.getString("location"),
                rs.getInt("created_by"),
                rs.getString("status"));
    }
}
