package com.student_study_group_finder.ui;

import com.student_study_group_finder.dao.*;
import com.student_study_group_finder.model.*;

import java.util.List;
import java.util.Scanner;

/**
 * Student role menu: Manage personal profile, view/search study groups,
 * join/leave study groups, view group members, and view scheduled study sessions.
 */
public class StudentMenu {
    private final Student student;
    private final StudentDao studentDao;
    private final StudyGroupDao studyGroupDao;
    private final GroupMembershipDao membershipDao;
    private final StudySessionDao sessionDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public StudentMenu(Student student, StudentDao studentDao, StudyGroupDao studyGroupDao,
                        GroupMembershipDao membershipDao, StudySessionDao sessionDao,
                        ActivityLogDao activityLogDao, Scanner scanner) {
        this.student = student;
        this.studentDao = studentDao;
        this.studyGroupDao = studyGroupDao;
        this.membershipDao = membershipDao;
        this.sessionDao = sessionDao;
        this.activityLogDao = activityLogDao;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("\n=========================================");
            System.out.println("                STUDENT MENU                ");
            System.out.println("=========================================");
            System.out.println("[1] View/Manage My Profile");
            System.out.println("[2] Browse & Join Study Groups");
            System.out.println("[3] My Joined Groups");
            System.out.println("[4] View Scheduled Study Sessions");
            System.out.println("[5] Logout");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewManageProfile();
                case "2" -> browseAndJoinGroups();
                case "3" -> myJoinedGroups();
                case "4" -> viewSessions();
                case "5" -> {
                    java.time.LocalDateTime logoutTime = java.time.LocalDateTime.now();
                    activityLogDao.logActivity(student.studentId(), "LOGOUT",
                            "User logged out", logoutTime.toLocalDate().toString(), logoutTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
                    System.out.println("Logging out. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please enter 1-5.");
            }
        }
    }

    /** View and manage personal profile. */
    private void viewManageProfile() {
        System.out.println("\n--- MY PROFILE ---");
        String course = student.course() != null ? truncate(student.course(), 30) : "N/A";
        String yearLevel = student.yearLevel() != null ? String.valueOf(student.yearLevel()) : "N/A";
        // Layout help: "%-14s" = text column 14 wide ("%-30s" = 30 wide).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Field", "Details");
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Student ID", String.valueOf(student.studentId()));
        System.out.printf("| %-14s | %-30s |\n", "Full Name", truncate(student.fullName(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Email", truncate(student.email(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Role", student.role());
        System.out.printf("| %-14s | %-30s |\n", "Course", course);
        System.out.printf("| %-14s | %-30s |\n", "Year Level", yearLevel);
        System.out.printf("| %-14s | %-30s |\n", "Account Status", student.accountStatus());
        System.out.printf("| %-14s | %-30s |\n", "Registered", student.dateRegistered() != null ? student.dateRegistered() : "N/A");
        System.out.println("+----------------+--------------------------------+");

        System.out.println("\n[1] Update Profile");
        System.out.println("[2] Change Password");
        System.out.println("[3] Back");
        System.out.print("Select an option: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            updateProfile();
        } else if (choice.equals("2")) {
            changePassword();
        } else if (!choice.equals("3")) {
            System.out.println("Invalid selection.");
        }
    }

    /** Changes the account password after confirming the current one. */
    private void changePassword() {
        System.out.println("\n--- CHANGE PASSWORD ---");
        System.out.print("Current Password: ");
        String current = scanner.nextLine().trim();
        if (!studentDao.checkPassword(student.studentId(), current)) {
            System.out.println("❌ Current password is wrong. Cancelled.");
            return;
        }

        System.out.print("New Password (at least 4 characters): ");
        String next = scanner.nextLine().trim();
        if (next.length() < 4) {
            System.out.println("Password is too short. Cancelled.");
            return;
        }

        System.out.print("Confirm New Password: ");
        String confirm = scanner.nextLine().trim();
        if (!next.equals(confirm)) {
            System.out.println("Passwords do not match. Cancelled.");
            return;
        }

        if (studentDao.updatePassword(student.studentId(), next)) {
            System.out.println("✅ Password changed successfully!");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            activityLogDao.logActivity(student.studentId(), "UPDATE_PASSWORD",
                    "Changed account password", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to change password.");
        }
    }

    /** Update personal profile information. */
    private void updateProfile() {
        System.out.println("\n--- UPDATE PROFILE ---");
        System.out.print("New Full Name (or press Enter to keep): ");
        String newName = scanner.nextLine().trim();
        if (newName.isEmpty()) newName = student.fullName();

        System.out.print("New Course (or press Enter to keep): ");
        String newCourse = scanner.nextLine().trim();
        if (newCourse.isEmpty()) newCourse = student.course();

        System.out.print("New Year Level (or press Enter to keep): ");
        String newYearLevelStr = scanner.nextLine().trim();
        Integer newYearLevel = student.yearLevel();
        if (!newYearLevelStr.isEmpty()) {
            try {
                newYearLevel = Integer.parseInt(newYearLevelStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Keeping current value.");
            }
        }

        boolean success = studentDao.updateProfile(student.studentId(), newName, newCourse, newYearLevel);

        if (success) {
            System.out.println("✅ Profile updated successfully!");
            java.time.LocalDateTime now2 = java.time.LocalDateTime.now();
            activityLogDao.logActivity(student.studentId(), "UPDATE_PROFILE",
                    "Updated profile information", now2.toLocalDate().toString(), now2.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to update profile.");
        }
    }

    /** Combined: Browse, search, and join study groups. */
    private void browseAndJoinGroups() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- BROWSE & JOIN STUDY GROUPS ---");
            System.out.println("[1] View All Study Groups");
            System.out.println("[2] Search Study Groups");
            System.out.println("[3] Join a Study Group");
            System.out.println("[4] View Group Details");
            System.out.println("[5] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllGroups();
                case "2" -> searchGroups();
                case "3" -> joinGroup();
                case "4" -> viewGroupDetails();
                case "5" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    /** View all active study groups. */
    private void viewAllGroups() {
        System.out.println("\n--- ALL STUDY GROUPS ---");
        List<StudyGroup> groups = studyGroupDao.getActiveGroups();

        if (groups.isEmpty()) {
            System.out.println("No study groups available.");
            return;
        }

        printGroupTable(groups);
    }

    /** Search study groups by keyword. */
    private void searchGroups() {
        System.out.println("\n--- SEARCH STUDY GROUPS ---");
        System.out.print("Enter search keyword (subject or group name): ");
        String query = scanner.nextLine().trim();

        if (query.isEmpty()) {
            System.out.println("Search cancelled.");
            return;
        }

        List<StudyGroup> groups = studyGroupDao.searchGroups(query);

        if (groups.isEmpty()) {
            System.out.println("No study groups found matching \"" + query + "\".");
            return;
        }

        System.out.println("Search results for \"" + query + "\":");
        printGroupTable(groups);
    }

    /** Shows full information for one group so students can decide before joining. */
    private void viewGroupDetails() {
        System.out.println("\n--- STUDY GROUP DETAILS ---");
        System.out.print("Enter Group ID (0 to cancel): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }

        int groupId;
        try {
            groupId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        StudyGroup group = studyGroupDao.getGroupById(groupId);
        if (group == null) {
            System.out.println("❌ Group not found.");
            return;
        }

        int members = studyGroupDao.getMemberCount(group.groupId());
        Student leader = findStudentById(group.createdBy());
        String description = group.description() != null ? truncate(group.description(), 30) : "N/A";
        String schedule = group.schedule() != null ? truncate(group.schedule(), 30) : "N/A";
        String leaderName = leader != null ? truncate(leader.fullName(), 30) : "N/A";
        // Layout help: "%-14s" = text column 14 wide ("%-30s" = 30 wide).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Field", "Details");
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Group Name", truncate(group.groupName(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Subject", truncate(group.subject(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Description", description);
        System.out.printf("| %-14s | %-30s |\n", "Schedule", schedule);
        System.out.printf("| %-14s | %-30s |\n", "Members", members + "/" + group.maxMembers());
        System.out.printf("| %-14s | %-30s |\n", "Status", group.status());
        System.out.printf("| %-14s | %-30s |\n", "Group Leader", leaderName);
        System.out.println("+----------------+--------------------------------+");
    }

    /** Combined: View joined groups, leave group, and view group members. */
    private void myJoinedGroups() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- MY JOINED GROUPS ---");
            System.out.println("[1] View My Groups");
            System.out.println("[2] Leave a Study Group");
            System.out.println("[3] View Group Members");
            System.out.println("[4] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewMyGroups();
                case "2" -> leaveGroup();
                case "3" -> viewGroupMembers();
                case "4" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    /** View groups the student has joined. */
    private void viewMyGroups() {
        System.out.println("\n--- MY JOINED GROUPS ---");
        List<GroupMembership> memberships = membershipDao.getStudentGroups(student.studentId());

        if (memberships.isEmpty()) {
            System.out.println("You are not a member of any study groups.");
            return;
        }

        // Layout help: "%-16s" = text column 16 wide ("%-6d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+----------+------------------+--------------+--------------+");
        System.out.printf("| %-8s | %-16s | %-12s | %-12s |\n", "Group ID", "Group Name", "My Role", "Joined");
        System.out.println("+----------+------------------+--------------+--------------+");

        for (GroupMembership membership : memberships) {
            StudyGroup group = studyGroupDao.getGroupById(membership.groupId());
            if (group != null) {
                String name = truncate(group.groupName(), 16);
                String joined = membership.joinDate() != null ? truncate(membership.joinDate(), 10) : "N/A";
                System.out.printf("| %-8d | %-16s | %-12s | %-12s |\n",
                        group.groupId(), name, membership.role(), joined);
            }
        }

        System.out.println("+----------+------------------+--------------+--------------+");
        System.out.println("Total: " + memberships.size() + " group(s)");
    }

    /** Join a study group by ID. */
    private void joinGroup() {
        System.out.println("\n--- JOIN A STUDY GROUP ---");
        System.out.print("Enter Group ID to join (0 to cancel): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Join cancelled.");
            return;
        }

        int groupId;
        try {
            groupId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Join cancelled.");
            return;
        }

        StudyGroup group = studyGroupDao.getGroupById(groupId);
        if (group == null) {
            System.out.println("❌ Group ID " + groupId + " does not exist.");
            return;
        }

        if (!"ACTIVE".equalsIgnoreCase(group.status())) {
            System.out.println("❌ This group is not active.");
            return;
        }

        if (membershipDao.isMember(groupId, student.studentId())) {
            System.out.println("❌ You are already a member of this group.");
            return;
        }

        int memberCount = studyGroupDao.getMemberCount(groupId);
        if (memberCount >= group.maxMembers()) {
            System.out.println("❌ Group is full (" + memberCount + "/" + group.maxMembers() + ").");
            return;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String joinDate = now.toLocalDate().toString();
        String joinTime = now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        boolean success = membershipDao.joinGroup(groupId, student.studentId(), joinDate, joinTime);

        if (success) {
            System.out.println("✅ Successfully joined \"" + group.groupName() + "\"!");
            activityLogDao.logActivity(student.studentId(), "JOIN_GROUP",
                    "Joined group: " + group.groupName() + " (group ID " + groupId + ")", joinDate, joinTime);
        } else {
            System.out.println("❌ Failed to join group. Please try again.");
        }
    }

    /** Leave a study group. */
    private void leaveGroup() {
        System.out.println("\n--- LEAVE A STUDY GROUP ---");
        List<GroupMembership> memberships = membershipDao.getStudentGroups(student.studentId());

        if (memberships.isEmpty()) {
            System.out.println("You are not a member of any study groups.");
            return;
        }

        System.out.println("Your groups:");
        for (GroupMembership membership : memberships) {
            StudyGroup group = studyGroupDao.getGroupById(membership.groupId());
            if (group != null) {
                System.out.println("  [" + group.groupId() + "] " + group.groupName() + " (" + group.subject() + ")");
            }
        }

        System.out.print("Enter Group ID to leave (0 to cancel): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Leave cancelled.");
            return;
        }

        int groupId;
        try {
            groupId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Leave cancelled.");
            return;
        }

        if (!membershipDao.isMember(groupId, student.studentId())) {
            System.out.println("❌ You are not a member of this group.");
            return;
        }

        boolean success = membershipDao.leaveGroup(groupId, student.studentId());
        if (success) {
            StudyGroup group = studyGroupDao.getGroupById(groupId);
            String groupName = group != null ? group.groupName() : String.valueOf(groupId);
            System.out.println("✅ Successfully left \"" + groupName + "\".");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            activityLogDao.logActivity(student.studentId(), "LEAVE_GROUP",
                    "Left group: " + groupName + " (group ID " + groupId + ")", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to leave group.");
        }
    }

    /** View members of a specific group. */
    private void viewGroupMembers() {
        System.out.println("\n--- VIEW GROUP MEMBERS ---");
        System.out.print("Enter Group ID (0 to cancel): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }

        int groupId;
        try {
            groupId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        StudyGroup group = studyGroupDao.getGroupById(groupId);
        if (group == null) {
            System.out.println("❌ Group not found.");
            return;
        }

        System.out.println("\nMembers of \"" + group.groupName() + "\":");
        List<GroupMembership> members = membershipDao.getGroupMembers(groupId);

        if (members.isEmpty()) {
            System.out.println("No members found.");
            return;
        }

        // Layout help: "%-13s" = text column 13 wide ("%-8d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+----------+---------------+--------------+--------------+");
        System.out.printf("| %-8s | %-13s | %-12s | %-12s |\n", "User ID", "Name", "Role", "Joined");
        System.out.println("+----------+---------------+--------------+--------------+");

        for (GroupMembership membership : members) {
            Student member = findStudentById(membership.studentId());
            String name = member != null ? truncate(member.fullName(), 13) : "Unknown";
            String joined = membership.joinDate() != null ? truncate(membership.joinDate(), 10) : "N/A";
            System.out.printf("| %-8d | %-13s | %-12s | %-12s |\n",
                    membership.studentId(), name, membership.role(), joined);
        }

        System.out.println("+----------+---------------+--------------+--------------+");
        System.out.println("Total: " + members.size() + " member(s)");
    }

    /** View scheduled study sessions for groups the student is in. */
    private void viewSessions() {
        System.out.println("\n--- SCHEDULED STUDY SESSIONS ---");
        List<GroupMembership> memberships = membershipDao.getStudentGroups(student.studentId());

        if (memberships.isEmpty()) {
            System.out.println("You are not a member of any study groups.");
            return;
        }

        boolean foundAny = false;
        for (GroupMembership membership : memberships) {
            StudyGroup group = studyGroupDao.getGroupById(membership.groupId());
            if (group == null) continue;

            List<StudySession> sessions = sessionDao.getSessionsByGroup(membership.groupId());
            if (sessions.isEmpty()) continue;

            if (!foundAny) {
                foundAny = true;
            }

            System.out.println("\n[" + group.groupName() + "] Sessions:");
            // Layout help: "%-10s" = text column 10 wide ("%-8d" = number column).
            // Each +---+ border piece needs width + 2 dashes. Change both together.
            System.out.println("+----------+------------+------------+------------+------------+------------+");
            System.out.printf("| %-8s | %-10s | %-10s | %-10s | %-10s | %-10s |\n",
                    "ID", "Date", "Start", "End", "Location", "Status");
            System.out.println("+----------+------------+------------+------------+------------+------------+");

            for (StudySession session : sessions) {
                String date = truncate(session.sessionDate(), 10);
                String start = truncate(session.startTime(), 10);
                String end = truncate(session.endTime(), 10);
                String location = session.location() != null ? truncate(session.location(), 10) : "N/A";
                System.out.printf("| %-8d | %-10s | %-10s | %-10s | %-10s | %-10s |\n",
                        session.sessionId(), date, start, end, location, session.status());
            }
            System.out.println("+----------+------------+------------+------------+------------+------------+");
        }

        if (!foundAny) {
            System.out.println("No scheduled sessions in your groups.");
        }
    }

    /** Prints a formatted table of study groups. */
    private void printGroupTable(List<StudyGroup> groups) {
        // Layout help: "%-16s" = text column 16 wide ("%-6d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+--------+------------------+----------------+------------+----------+");
        System.out.printf("| %-6s | %-16s | %-14s | %-10s | %-8s |\n",
                "ID", "Group Name", "Subject", "Members", "Status");
        System.out.println("+--------+------------------+----------------+------------+----------+");

        for (StudyGroup group : groups) {
            String name = truncate(group.groupName(), 16);
            String subject = truncate(group.subject(), 12);
            int count = studyGroupDao.getMemberCount(group.groupId());
            System.out.printf("| %-6d | %-16s | %-14s | %-10d | %-8s |\n",
                    group.groupId(), name, subject, count, group.status());
        }

        System.out.println("+--------+------------------+----------------+------------+----------+");
        System.out.println("Total: " + groups.size() + " group(s)");
    }

    /** Helper to find a student by ID. */
    private Student findStudentById(int id) {
        List<Student> all = studentDao.getAllUsers();
        for (Student s : all) {
            if (s.studentId() == id) return s;
        }
        return null;
    }

    /** Truncates a string to the specified length. */
    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
}
