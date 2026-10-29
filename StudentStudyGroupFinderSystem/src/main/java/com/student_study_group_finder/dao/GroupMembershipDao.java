package com.student_study_group_finder.dao;

import com.student_study_group_finder.model.GroupMembership;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All SQL for the group_memberships table. Every query uses PreparedStatement.
 */
public class GroupMembershipDao {

    /** Adds a student to a study group as a MEMBER. */
    public boolean joinGroup(int groupId, int studentId, String joinDate, String joinTime) {
        String sql = """
                INSERT INTO group_memberships (group_id, student_id, role, join_date, join_time, status)
                VALUES (?, ?, 'MEMBER', ?, ?, 'ACTIVE')
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, studentId);
            ps.setString(3, joinDate);
            ps.setString(4, joinTime);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to join group: " + e.getMessage());
            return false;
        }
    }

    /** Adds the group creator as the LEADER of the group. */
    public boolean addLeader(int groupId, int studentId, String joinDate, String joinTime) {
        String sql = """
                INSERT INTO group_memberships (group_id, student_id, role, join_date, join_time, status)
                VALUES (?, ?, 'LEADER', ?, ?, 'ACTIVE')
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, studentId);
            ps.setString(3, joinDate);
            ps.setString(4, joinTime);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to add leader: " + e.getMessage());
            return false;
        }
    }

    /** Checks if a student is already a member of a group. */
    public boolean isMember(int groupId, int studentId) {
        String sql = "SELECT 1 FROM group_memberships WHERE group_id = ? AND student_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
            return false;
        }
    }

    /** Removes a student from a group (marks as LEFT). */
    public boolean leaveGroup(int groupId, int studentId) {
        String sql = "UPDATE group_memberships SET status = 'LEFT' WHERE group_id = ? AND student_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, studentId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to leave group: " + e.getMessage());
            return false;
        }
    }

    /** Removes a student from a group (marks as REMOVED) — for leader/admin use. */
    public boolean removeMember(int groupId, int studentId) {
        String sql = "UPDATE group_memberships SET status = 'REMOVED' WHERE group_id = ? AND student_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, studentId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to remove member: " + e.getMessage());
            return false;
        }
    }

    /** Returns all active members of a group. */
    public List<GroupMembership> getGroupMembers(int groupId) {
        String sql = "SELECT membership_id, group_id, student_id, role, join_date, join_time, status FROM group_memberships WHERE group_id = ? AND status = 'ACTIVE'";
        List<GroupMembership> members = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return members;
    }

    /** Returns all groups a student is actively member of. */
    public List<GroupMembership> getStudentGroups(int studentId) {
        String sql = "SELECT membership_id, group_id, student_id, role, join_date, join_time, status FROM group_memberships WHERE student_id = ? AND status = 'ACTIVE'";
        List<GroupMembership> memberships = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    memberships.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return memberships;
    }

    /** Returns all memberships for a group (including non-active for admin view). */
    public List<GroupMembership> getAllGroupMembers(int groupId) {
        String sql = "SELECT membership_id, group_id, student_id, role, join_date, join_time, status FROM group_memberships WHERE group_id = ?";
        List<GroupMembership> members = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
        return members;
    }

    /** Updates a member's role (e.g., promote to CO_LEADER). */
    public boolean updateMemberRole(int groupId, int studentId, String role) {
        String sql = "UPDATE group_memberships SET role = ? WHERE group_id = ? AND student_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, groupId);
            ps.setInt(3, studentId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to update member role: " + e.getMessage());
            return false;
        }
    }

    /** Deletes a membership record entirely (admin use). */
    public boolean deleteMembership(int groupId, int studentId) {
        String sql = "DELETE FROM group_memberships WHERE group_id = ? AND student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.setInt(2, studentId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            System.out.println("Failed to delete membership: " + e.getMessage());
            return false;
        }
    }

    private GroupMembership mapResultSet(ResultSet rs) throws SQLException {
        return new GroupMembership(
                rs.getInt("membership_id"),
                rs.getInt("group_id"),
                rs.getInt("student_id"),
                rs.getString("role"),
                rs.getString("join_date"),
                rs.getString("join_time"),
                rs.getString("status"));
    }
}
