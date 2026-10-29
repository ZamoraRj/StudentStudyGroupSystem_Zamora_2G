package com.student_study_group_finder.dao;

import com.student_study_group_finder.model.StudyGroup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the study_groups table. Every query uses PreparedStatement.
 */
public class StudyGroupDao {

    /** Creates a new study group. */
    public boolean createGroup(String groupName, String description, String subject,
                                String schedule, int maxMembers, int createdBy) {
        String sql = """
                INSERT INTO study_groups (group_name, description, subject, schedule, max_members, created_by, status)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupName);
            if (description == null || description.isEmpty()) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, description);
            }
            ps.setString(3, subject);
            if (schedule == null || schedule.isEmpty()) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, schedule);
            }
            ps.setInt(5, maxMembers);
            ps.setInt(6, createdBy);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to create group: " + e.getMessage());
            return false;
        }
    }

    /** Returns all study groups. */
    public List<StudyGroup> getAllGroups() {
        String sql = "SELECT group_id, group_name, description, subject, schedule, max_members, created_by, status FROM study_groups";
        List<StudyGroup> groups = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return groups;
    }

    /** Returns all active study groups. */
    public List<StudyGroup> getActiveGroups() {
        String sql = "SELECT group_id, group_name, description, subject, schedule, max_members, created_by, status FROM study_groups WHERE status = ?";
        List<StudyGroup> groups = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return groups;
    }

    /** Searches study groups by subject or group name. */
    public List<StudyGroup> searchGroups(String query) {
        String sql = "SELECT group_id, group_name, description, subject, schedule, max_members, created_by, status " +
                     "FROM study_groups WHERE status = ? AND (group_name LIKE ? OR subject LIKE ?)";
        List<StudyGroup> groups = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "ACTIVE");
            String likePattern = "%" + query + "%";
            ps.setString(2, likePattern);
            ps.setString(3, likePattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return groups;
    }

    /** Returns a single group by ID. */
    public StudyGroup getGroupById(int groupId) {
        String sql = "SELECT group_id, group_name, description, subject, schedule, max_members, created_by, status FROM study_groups WHERE group_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
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

    /** Returns groups created by a specific user. */
    public List<StudyGroup> getGroupsByLeader(int createdBy) {
        String sql = "SELECT group_id, group_name, description, subject, schedule, max_members, created_by, status FROM study_groups WHERE created_by = ?";
        List<StudyGroup> groups = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, createdBy);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return groups;
    }

    /** Updates group information. */
    public boolean updateGroup(int groupId, String groupName, String description, String subject, String schedule, int maxMembers) {
        String sql = "UPDATE study_groups SET group_name = ?, description = ?, subject = ?, schedule = ?, max_members = ? WHERE group_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, groupName);
            if (description == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, description);
            }
            ps.setString(3, subject);
            if (schedule == null) {
                ps.setNull(4, Types.VARCHAR);
            } else {
                ps.setString(4, schedule);
            }
            ps.setInt(5, maxMembers);
            ps.setInt(6, groupId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to update group: " + e.getMessage());
            return false;
        }
    }

    /** Updates group status (ACTIVE, INACTIVE, ARCHIVED). */
    public boolean updateGroupStatus(int groupId, String status) {
        String sql = "UPDATE study_groups SET status = ? WHERE group_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, groupId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to update group status: " + e.getMessage());
            return false;
        }
    }

    /** Deletes a study group. */
    public boolean deleteGroup(int groupId) {
        String sql = "DELETE FROM study_groups WHERE group_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to delete group: " + e.getMessage());
            return false;
        }
    }

    /** Returns the current member count for a group. */
    public int getMemberCount(int groupId) {
        String sql = "SELECT COUNT(*) AS cnt FROM group_memberships WHERE group_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("cnt");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return 0;
    }

    private StudyGroup mapResultSet(ResultSet rs) throws SQLException {
        return new StudyGroup(
                rs.getInt("group_id"),
                rs.getString("group_name"),
                rs.getString("description"),
                rs.getString("subject"),
                rs.getString("schedule"),
                rs.getInt("max_members"),
                rs.getInt("created_by"),
                rs.getString("status"));
    }
}
