package com.student_study_group_finder.ui;

import com.student_study_group_finder.dao.*;
import com.student_study_group_finder.model.*;

import java.util.List;
import java.util.Scanner;

/**
 * Admin menu for managing users, study groups, group memberships,
 * study sessions, summary reports, and activity logs.
 */
public class AdminMenu {
    private final Student currentAdmin;
    private final StudentDao studentDao;
    private final StudyGroupDao studyGroupDao;
    private final GroupMembershipDao membershipDao;
    private final StudySessionDao sessionDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public AdminMenu(Student currentAdmin, StudentDao studentDao, StudyGroupDao studyGroupDao,
                      GroupMembershipDao membershipDao, StudySessionDao sessionDao,
                      ActivityLogDao activityLogDao, Scanner scanner) {
        this.currentAdmin = currentAdmin;
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
            System.out.println("             ADMINISTRATION MENU             ");
            System.out.println("=========================================");
            System.out.println("[1] Manage Users");
            System.out.println("[2] Manage Study Groups");
            System.out.println("[3] Manage Group Memberships");
            System.out.println("[4] Manage Study Sessions");
            System.out.println("[5] Summary Reports");
            System.out.println("[6] Activity Logs");
            System.out.println("[7] My Profile");
            System.out.println("[8] Logout");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> userManagementMenu();
                case "2" -> groupManagementMenu();
                case "3" -> membershipManagementMenu();
                case "4" -> sessionManagementMenu();
                case "5" -> summaryReports();
                case "6" -> activityLogsMenu();
                case "7" -> viewMyProfile();
                case "8" -> {
                    java.time.LocalDateTime logoutTime = java.time.LocalDateTime.now();
                    activityLogDao.logActivity(currentAdmin.studentId(), "LOGOUT",
                            "User logged out", logoutTime.toLocalDate().toString(), logoutTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
                    System.out.println("Logging out. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please enter 1-8.");
            }
        }
    }

    /** View and manage the logged-in admin's own profile. */
    private void viewMyProfile() {
        System.out.println("\n--- MY PROFILE ---");
        Student admin = studentDao.getUserById(currentAdmin.studentId());
        if (admin == null) {
            System.out.println("Profile not found.");
            return;
        }

        String course = admin.course() != null ? truncate(admin.course(), 30) : "N/A";
        String yearLevel = admin.yearLevel() != null ? String.valueOf(admin.yearLevel()) : "N/A";
        String registered = admin.dateRegistered() != null ? admin.dateRegistered() : "N/A";
        // Layout help: "%-14s" = text column 14 wide ("%-30s" = 30 wide).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Field", "Details");
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Student ID", String.valueOf(admin.studentId()));
        System.out.printf("| %-14s | %-30s |\n", "Full Name", truncate(admin.fullName(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Email", truncate(admin.email(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Role", admin.role());
        System.out.printf("| %-14s | %-30s |\n", "Course", course);
        System.out.printf("| %-14s | %-30s |\n", "Year Level", yearLevel);
        System.out.printf("| %-14s | %-30s |\n", "Account Status", admin.accountStatus());
        System.out.printf("| %-14s | %-30s |\n", "Registered", registered);
        System.out.println("+----------------+--------------------------------+");

        System.out.println("\n[1] Update Profile");
        System.out.println("[2] Change Password");
        System.out.println("[3] Back");
        System.out.print("Select an option: ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("1")) {
            updateMyProfile();
        } else if (choice.equals("2")) {
            changeMyPassword();
        } else if (!choice.equals("3")) {
            System.out.println("Invalid selection.");
        }
    }

    /** Update the logged-in admin's own profile information. */
    private void updateMyProfile() {
        System.out.println("\n--- UPDATE PROFILE ---");
        System.out.print("New Full Name (or press Enter to keep): ");
        String newName = scanner.nextLine().trim();
        if (newName.isEmpty()) newName = currentAdmin.fullName();

        System.out.print("New Course (or press Enter to keep): ");
        String newCourse = scanner.nextLine().trim();
        if (newCourse.isEmpty()) newCourse = currentAdmin.course();

        System.out.print("New Year Level (or press Enter to keep): ");
        String newYearLevelStr = scanner.nextLine().trim();
        Integer newYearLevel = currentAdmin.yearLevel();
        if (!newYearLevelStr.isEmpty()) {
            try {
                newYearLevel = Integer.parseInt(newYearLevelStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Keeping current value.");
            }
        }

        if (studentDao.updateProfile(currentAdmin.studentId(), newName, newCourse, newYearLevel)) {
            System.out.println("✅ Profile updated successfully!");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "UPDATE_PROFILE",
                    "Updated profile information", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to update profile.");
        }
    }

    /** Change the logged-in admin's own password after confirming the current one. */
    private void changeMyPassword() {
        System.out.println("\n--- CHANGE PASSWORD ---");
        System.out.print("Current Password: ");
        String current = scanner.nextLine().trim();
        if (!studentDao.checkPassword(currentAdmin.studentId(), current)) {
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

        if (studentDao.updatePassword(currentAdmin.studentId(), next)) {
            System.out.println("✅ Password changed successfully!");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "UPDATE_PASSWORD",
                    "Changed account password", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to change password.");
        }
    }

    // ==================== USER MANAGEMENT ====================

    private void userManagementMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- USER MANAGEMENT ---");
            System.out.println("[1] View All System Users");
            System.out.println("[2] View Pending Registrations");
            System.out.println("[3] Approve User Account");
            System.out.println("[4] Deactivate User Account");
            System.out.println("[5] Reset User Password");
            System.out.println("[6] View User Profile");
            System.out.println("[7] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllUsers();
                case "2" -> viewPendingRegistrations();
                case "3" -> approveUserAccount();
                case "4" -> deactivateUserAccount();
                case "5" -> resetUserPassword();
                case "6" -> viewUserProfile();
                case "7" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    /** Displays all registered users in a formatted table. */
    private void viewAllUsers() {
        System.out.println("\n--- ALL SYSTEM USERS ---");
        List<Student> users = studentDao.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        printUserTable(users);
    }

    /** Displays all pending (unapproved) users in a formatted table. */
    private void viewPendingRegistrations() {
        System.out.println("\n--- PENDING REGISTRATIONS ---");
        List<Student> users = studentDao.getPendingUsers();

        if (users.isEmpty()) {
            System.out.println("No pending registrations.");
            return;
        }

        printUserTable(users);
    }

    /** Prompts for a Target User ID, verifies it exists in pending list, and approves the account. */
    private void approveUserAccount() {
        System.out.println("\n--- APPROVE USER ACCOUNT ---");
        List<Student> pending = studentDao.getPendingUsers();

        if (pending.isEmpty()) {
            System.out.println("No pending accounts to approve.");
            return;
        }

        System.out.println("Pending accounts:");
        printUserTable(pending);

        System.out.print("Enter Target User ID to approve (0 to cancel): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Approval cancelled.");
            return;
        }

        int targetId;
        try {
            targetId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Approval cancelled.");
            return;
        }

        boolean found = false;
        for (Student user : pending) {
            if (user.studentId() == targetId) {
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("User ID " + targetId + " is not in the pending list. Approval cancelled.");
            return;
        }

        boolean success = studentDao.approveUser(targetId);
        if (success) {
            java.time.LocalDateTime approveTime = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "APPROVE_USER",
                    "Approved user account (user ID " + targetId + ")", approveTime.toLocalDate().toString(), approveTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            System.out.println("✅ User ID " + targetId + " has been approved successfully.");
        } else {
            System.out.println("❌ Failed to approve User ID " + targetId + ".");
        }
    }

    /** Prompts for a Target User ID and deactivates the account. */
    private void deactivateUserAccount() {
        System.out.println("\n--- DEACTIVATE USER ACCOUNT ---");
        List<Student> users = studentDao.getAllUsers();

        // Filter out already deactivated and pending users
        List<Student> activeUsers = new java.util.ArrayList<>();
        for (Student user : users) {
            if ("APPROVED".equalsIgnoreCase(user.accountStatus())) {
                activeUsers.add(user);
            }
        }

        if (activeUsers.isEmpty()) {
            System.out.println("No active accounts to deactivate.");
            return;
        }

        System.out.println("Active accounts:");
        printUserTable(activeUsers);

        System.out.print("Enter Target User ID to deactivate (0 to cancel): ");
        String input = scanner.nextLine().trim();

        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Deactivation cancelled.");
            return;
        }

        int targetId;
        try {
            targetId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Deactivation cancelled.");
            return;
        }

        boolean found = false;
        for (Student user : activeUsers) {
            if (user.studentId() == targetId) {
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("User ID " + targetId + " is not in the active list. Deactivation cancelled.");
            return;
        }

        boolean success = studentDao.deactivateAccount(targetId);
        if (success) {
            java.time.LocalDateTime deactivateTime = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "DEACTIVATE_USER",
                    "Deactivated user account (user ID " + targetId + ")", deactivateTime.toLocalDate().toString(), deactivateTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            System.out.println("✅ User ID " + targetId + " has been deactivated successfully.");
        } else {
            System.out.println("❌ Failed to deactivate User ID " + targetId + ".");
        }
    }

    /** Prompts for a Target User ID and sets a new password for that account. */
    private void resetUserPassword() {
        System.out.println("\n--- RESET USER PASSWORD ---");
        List<Student> users = studentDao.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        printUserTable(users);

        System.out.print("Enter Target User ID (0 to cancel): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Reset cancelled.");
            return;
        }

        int targetId;
        try {
            targetId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Reset cancelled.");
            return;
        }

        boolean found = false;
        for (Student user : users) {
            if (user.studentId() == targetId) {
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("User ID " + targetId + " does not exist. Reset cancelled.");
            return;
        }

        System.out.print("New Password (at least 4 characters): ");
        String next = scanner.nextLine().trim();
        if (next.length() < 4) {
            System.out.println("Password is too short. Reset cancelled.");
            return;
        }

        if (studentDao.updatePassword(targetId, next)) {
            System.out.println("✅ Password for User ID " + targetId + " has been reset.");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "RESET_PASSWORD",
                    "Reset password for user ID " + targetId,
                    now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to reset password.");
        }
    }

    /** Shows the full profile of one selected user. */
    private void viewUserProfile() {
        System.out.println("\n--- VIEW USER PROFILE ---");
        List<Student> users = studentDao.getAllUsers();

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        printUserTable(users);

        System.out.print("Enter Target User ID (0 to cancel): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }

        int targetId;
        try {
            targetId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        Student target = null;
        for (Student user : users) {
            if (user.studentId() == targetId) {
                target = user;
                break;
            }
        }

        if (target == null) {
            System.out.println("User ID " + targetId + " does not exist.");
            return;
        }

        String course = target.course() != null ? truncate(target.course(), 30) : "N/A";
        String yearLevel = target.yearLevel() != null ? String.valueOf(target.yearLevel()) : "N/A";
        String registered = target.dateRegistered() != null ? target.dateRegistered() : "N/A";
        // Layout help: "%-14s" = text column 14 wide ("%-30s" = 30 wide).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Field", "Details");
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Student ID", String.valueOf(target.studentId()));
        System.out.printf("| %-14s | %-30s |\n", "Full Name", truncate(target.fullName(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Email", truncate(target.email(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Role", target.role());
        System.out.printf("| %-14s | %-30s |\n", "Course", course);
        System.out.printf("| %-14s | %-30s |\n", "Year Level", yearLevel);
        System.out.printf("| %-14s | %-30s |\n", "Account Status", target.accountStatus());
        System.out.printf("| %-14s | %-30s |\n", "Registered", registered);
        System.out.println("+----------------+--------------------------------+");
    }

    // ==================== GROUP MANAGEMENT ====================

    private void groupManagementMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- STUDY GROUP MANAGEMENT ---");
            System.out.println("[1] View All Study Groups");
            System.out.println("[2] Update Group Status");
            System.out.println("[3] Delete Study Group");
            System.out.println("[4] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllGroups();
                case "2" -> updateGroupStatus();
                case "3" -> deleteGroup();
                case "4" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    private void viewAllGroups() {
        System.out.println("\n--- ALL STUDY GROUPS ---");
        List<StudyGroup> groups = studyGroupDao.getAllGroups();

        if (groups.isEmpty()) {
            System.out.println("No study groups found.");
            return;
        }

        printGroupTable(groups);
    }

    private void updateGroupStatus() {
        System.out.println("\n--- UPDATE GROUP STATUS ---");
        List<StudyGroup> groups = studyGroupDao.getAllGroups();

        if (groups.isEmpty()) {
            System.out.println("No study groups found.");
            return;
        }

        printGroupTable(groups);

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

        System.out.println("Select new status:");
        System.out.println("1. ACTIVE");
        System.out.println("2. INACTIVE");
        System.out.println("3. ARCHIVED");
        System.out.print("Choose (1-3): ");
        String choice = scanner.nextLine().trim();

        String status = switch (choice) {
            case "1" -> "ACTIVE";
            case "2" -> "INACTIVE";
            case "3" -> "ARCHIVED";
            default -> null;
        };

        if (status == null) {
            System.out.println("Invalid selection.");
            return;
        }

        boolean success = studyGroupDao.updateGroupStatus(groupId, status);
        if (success) {
            java.time.LocalDateTime groupStatusTime = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "UPDATE_GROUP_STATUS",
                    "Status changed to " + status + " (group ID " + groupId + ")", groupStatusTime.toLocalDate().toString(), groupStatusTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            System.out.println("✅ Group status updated to " + status + ".");
        } else {
            System.out.println("❌ Failed to update group status.");
        }
    }

    private void deleteGroup() {
        System.out.println("\n--- DELETE STUDY GROUP ---");
        List<StudyGroup> groups = studyGroupDao.getAllGroups();

        if (groups.isEmpty()) {
            System.out.println("No study groups found.");
            return;
        }

        printGroupTable(groups);

        System.out.print("Enter Group ID to delete (0 to cancel): ");
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

        System.out.print("⚠️  This will delete the group and all its data. Type DELETE to confirm: ");
        String confirm = scanner.nextLine().trim();
        if (!"DELETE".equalsIgnoreCase(confirm)) {
            System.out.println("Deletion cancelled.");
            return;
        }

        boolean success = studyGroupDao.deleteGroup(groupId);
        if (success) {
            java.time.LocalDateTime deleteGroupTime = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "DELETE_GROUP",
                    "Deleted group (group ID " + groupId + ")", deleteGroupTime.toLocalDate().toString(), deleteGroupTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            System.out.println("✅ Group deleted successfully.");
        } else {
            System.out.println("❌ Failed to delete group.");
        }
    }

    // ==================== MEMBERSHIP MANAGEMENT ====================

    private void membershipManagementMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- GROUP MEMBERSHIP MANAGEMENT ---");
            System.out.println("[1] View Memberships by Group");
            System.out.println("[2] Remove Member from Group");
            System.out.println("[3] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewMembershipsByGroup();
                case "2" -> adminRemoveMember();
                case "3" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    private void viewMembershipsByGroup() {
        System.out.println("\n--- VIEW MEMBERSHIPS BY GROUP ---");
        List<StudyGroup> groups = studyGroupDao.getAllGroups();

        if (groups.isEmpty()) {
            System.out.println("No study groups found.");
            return;
        }

        System.out.println("Available groups:");
        for (StudyGroup g : groups) {
            System.out.println("  [" + g.groupId() + "] " + g.groupName() + " (" + g.status() + ")");
        }

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

        List<GroupMembership> members = membershipDao.getAllGroupMembers(groupId);
        if (members.isEmpty()) {
            System.out.println("No memberships found for this group.");
            return;
        }

        // Layout help: "%-10s" = text column 10 wide ("%-8d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("\n+------------+--------+----------+--------------+----------+");
        System.out.printf("| %-10s | %-6s | %-8s | %-12s | %-8s |\n",
                "Membership", "Group", "User ID", "Role", "Status");
        System.out.println("+------------+--------+----------+--------------+----------+");

        for (GroupMembership m : members) {
            System.out.printf("| %-10d | %-6d | %-8d | %-12s | %-8s |\n",
                    m.membershipId(), m.groupId(), m.studentId(), m.role(), m.status());
        }
        System.out.println("+------------+--------+----------+--------------+----------+");
        System.out.println("Total: " + members.size() + " membership(ies)");
    }

    private void adminRemoveMember() {
        System.out.println("\n--- REMOVE MEMBER FROM GROUP ---");
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

        System.out.print("Enter User ID to remove: ");
        String uid = scanner.nextLine().trim();
        if (uid.isEmpty() || uid.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }

        int userId;
        try {
            userId = Integer.parseInt(uid);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        boolean success = membershipDao.removeMember(groupId, userId);
        if (success) {
            java.time.LocalDateTime removeTime = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "REMOVE_MEMBER",
                    "Removed user " + userId + " from group (group ID " + groupId + ")", removeTime.toLocalDate().toString(), removeTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            System.out.println("✅ Member removed from group.");
        } else {
            System.out.println("❌ Failed to remove member. Check if they are an active member.");
        }
    }

    // ==================== SESSION MANAGEMENT ====================

    private void sessionManagementMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- STUDY SESSION MANAGEMENT ---");
            System.out.println("[1] View All Sessions");
            System.out.println("[2] Update Session Status");
            System.out.println("[3] Delete Session");
            System.out.println("[4] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllSessions();
                case "2" -> adminUpdateSessionStatus();
                case "3" -> adminDeleteSession();
                case "4" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    private void viewAllSessions() {
        System.out.println("\n--- ALL STUDY SESSIONS ---");
        List<StudySession> sessions = sessionDao.getAllSessions();

        if (sessions.isEmpty()) {
            System.out.println("No study sessions found.");
            return;
        }

        // Layout help: "%-10s" = text column 10 wide ("%-8d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+----------+--------+------------+------------+------------+------------+------------+");
        System.out.printf("| %-8s | %-6s | %-10s | %-10s | %-10s | %-10s | %-10s |\n",
                "ID", "Group", "Date", "Start", "End", "Location", "Status");
        System.out.println("+----------+--------+------------+------------+------------+------------+------------+");

        for (StudySession s : sessions) {
            String date = truncate(s.sessionDate(), 10);
            String start = truncate(s.startTime(), 10);
            String end = truncate(s.endTime(), 10);
            String loc = s.location() != null ? truncate(s.location(), 10) : "N/A";
            System.out.printf("| %-8d | %-6d | %-10s | %-10s | %-10s | %-10s | %-10s |\n",
                    s.sessionId(), s.groupId(), date, start, end, loc, s.status());
        }
        System.out.println("+----------+--------+------------+------------+------------+------------+------------+");
        System.out.println("Total: " + sessions.size() + " session(s)");
    }

    private void adminUpdateSessionStatus() {
        System.out.println("\n--- UPDATE SESSION STATUS ---");
        System.out.print("Enter Session ID (0 to cancel): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }

        int sessionId;
        try {
            sessionId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        System.out.println("Select new status:");
        System.out.println("1. SCHEDULED");
        System.out.println("2. IN_PROGRESS");
        System.out.println("3. COMPLETED");
        System.out.println("4. CANCELLED");
        System.out.print("Choose (1-4): ");
        String choice = scanner.nextLine().trim();

        String status = switch (choice) {
            case "1" -> "SCHEDULED";
            case "2" -> "IN_PROGRESS";
            case "3" -> "COMPLETED";
            case "4" -> "CANCELLED";
            default -> null;
        };

        if (status == null) {
            System.out.println("Invalid selection.");
            return;
        }

        boolean success = sessionDao.updateSessionStatus(sessionId, status);
        if (success) {
            java.time.LocalDateTime sessionStatusTime = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "UPDATE_SESSION_STATUS",
                    "Status changed to " + status + " (session ID " + sessionId + ")", sessionStatusTime.toLocalDate().toString(), sessionStatusTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            System.out.println("✅ Session status updated to " + status + ".");
        } else {
            System.out.println("❌ Failed to update session status.");
        }
    }

    private void adminDeleteSession() {
        System.out.println("\n--- DELETE SESSION ---");
        System.out.print("Enter Session ID to delete (0 to cancel): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty() || input.equals("0")) {
            System.out.println("Cancelled.");
            return;
        }

        int sessionId;
        try {
            sessionId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
            return;
        }

        boolean success = sessionDao.deleteSession(sessionId);
        if (success) {
            java.time.LocalDateTime deleteSessionTime = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "DELETE_SESSION",
                    "Deleted session (session ID " + sessionId + ")", deleteSessionTime.toLocalDate().toString(), deleteSessionTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            System.out.println("✅ Session deleted successfully.");
        } else {
            System.out.println("❌ Failed to delete session.");
        }
    }

    // ==================== SUMMARY REPORTS ====================

    private void summaryReports() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- SUMMARY REPORTS ---");
            System.out.println("[1] View Full Study Group Activity Report");
            System.out.println("[2] User Statistics");
            System.out.println("[3] Group Statistics");
            System.out.println("[4] Session Statistics");
            System.out.println("[5] Activity Summary");
            System.out.println("[6] Export Study Group Activity Report (CSV/PDF/XLSX)");
            System.out.println("[7] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewFullReport();
                case "2" -> userStatistics();
                case "3" -> groupStatistics();
                case "4" -> sessionStatistics();
                case "5" -> activitySummary();
                case "6" -> exportActivityReport();
                case "7" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    private void userStatistics() {
        System.out.println("\n--- USER STATISTICS ---");
        List<Student> allUsers = studentDao.getAllUsers();
        List<Student> pendingUsers = studentDao.getPendingUsers();

        long studentCount = allUsers.stream().filter(u -> "STUDENT".equalsIgnoreCase(u.role())).count();
        long leaderCount = allUsers.stream().filter(u -> "STUDY_GROUP_LEADER".equalsIgnoreCase(u.role())).count();
        long adminCount = allUsers.stream().filter(u -> "ADMINISTRATOR".equalsIgnoreCase(u.role())).count();

        System.out.println("Total Users: " + allUsers.size());
        System.out.println("  - Students: " + studentCount);
        System.out.println("  - Study Group Leaders: " + leaderCount);
        System.out.println("  - Administrators: " + adminCount);
        System.out.println("Pending Approvals: " + pendingUsers.size());
        System.out.println("Approved Users: " + (allUsers.size() - pendingUsers.size()));
    }

    private void groupStatistics() {
        System.out.println("\n--- GROUP STATISTICS ---");
        List<StudyGroup> allGroups = studyGroupDao.getAllGroups();

        long activeCount = allGroups.stream().filter(g -> "ACTIVE".equalsIgnoreCase(g.status())).count();
        long inactiveCount = allGroups.stream().filter(g -> "INACTIVE".equalsIgnoreCase(g.status())).count();
        long archivedCount = allGroups.stream().filter(g -> "ARCHIVED".equalsIgnoreCase(g.status())).count();

        int totalMembers = 0;
        for (StudyGroup g : allGroups) {
            totalMembers += studyGroupDao.getMemberCount(g.groupId());
        }

        System.out.println("Total Groups: " + allGroups.size());
        System.out.println("  - Active: " + activeCount);
        System.out.println("  - Inactive: " + inactiveCount);
        System.out.println("  - Archived: " + archivedCount);
        System.out.println("Total Active Memberships: " + totalMembers);
    }

    private void sessionStatistics() {
        System.out.println("\n--- SESSION STATISTICS ---");
        List<StudySession> allSessions = sessionDao.getAllSessions();

        long scheduled = allSessions.stream().filter(s -> "SCHEDULED".equalsIgnoreCase(s.status())).count();
        long inProgress = allSessions.stream().filter(s -> "IN_PROGRESS".equalsIgnoreCase(s.status())).count();
        long completed = allSessions.stream().filter(s -> "COMPLETED".equalsIgnoreCase(s.status())).count();
        long cancelled = allSessions.stream().filter(s -> "CANCELLED".equalsIgnoreCase(s.status())).count();

        System.out.println("Total Sessions: " + allSessions.size());
        System.out.println("  - Scheduled: " + scheduled);
        System.out.println("  - In Progress: " + inProgress);
        System.out.println("  - Completed: " + completed);
        System.out.println("  - Cancelled: " + cancelled);
    }

    private void activitySummary() {
        System.out.println("\n--- ACTIVITY SUMMARY ---");
        List<ActivityLogDao.ActionCount> summary = activityLogDao.getActionSummary();

        if (summary.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }

        // Layout help: "%-14s" = text column 14 wide ("%-6d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+------------------+--------+");
        System.out.printf("| %-16s | %-6s |\n", "Action", "Count");
        System.out.println("+------------------+--------+");

        for (ActivityLogDao.ActionCount ac : summary) {
            System.out.printf("| %-16s | %-6d |\n", truncate(ac.action(), 14), ac.count());
        }
        System.out.println("+------------------+--------+");
    }

    /** Builds every section of the Study Group Activity Report for console display and export. */
    private java.util.List<com.student_study_group_finder.report.ReportExporter.Section> buildActivityReport() {
        java.util.List<com.student_study_group_finder.report.ReportExporter.Section> sections = new java.util.ArrayList<>();

        List<Student> allUsers = studentDao.getAllUsers();
        List<Student> pendingUsers = studentDao.getPendingUsers();
        long studentCount = allUsers.stream().filter(u -> "STUDENT".equalsIgnoreCase(u.role())).count();
        long leaderCount = allUsers.stream().filter(u -> "STUDY_GROUP_LEADER".equalsIgnoreCase(u.role())).count();
        long adminCount = allUsers.stream().filter(u -> "ADMINISTRATOR".equalsIgnoreCase(u.role())).count();
        java.util.List<String[]> userRows = new java.util.ArrayList<>();
        userRows.add(new String[]{"Total Registered Students", String.valueOf(allUsers.size())});
        userRows.add(new String[]{"Students", String.valueOf(studentCount)});
        userRows.add(new String[]{"Study Group Leaders", String.valueOf(leaderCount)});
        userRows.add(new String[]{"Administrators", String.valueOf(adminCount)});
        userRows.add(new String[]{"Pending Approvals", String.valueOf(pendingUsers.size())});
        userRows.add(new String[]{"Approved Users", String.valueOf(allUsers.size() - pendingUsers.size())});
        sections.add(new com.student_study_group_finder.report.ReportExporter.Section("USER STATISTICS", userRows));

        List<StudyGroup> allGroups = studyGroupDao.getAllGroups();
        long activeGroups = allGroups.stream().filter(g -> "ACTIVE".equalsIgnoreCase(g.status())).count();
        long inactiveGroups = allGroups.stream().filter(g -> "INACTIVE".equalsIgnoreCase(g.status())).count();
        long archivedGroups = allGroups.stream().filter(g -> "ARCHIVED".equalsIgnoreCase(g.status())).count();
        int totalMembers = 0;
        double totalCapacity = 0;
        for (StudyGroup g : allGroups) {
            int members = studyGroupDao.getMemberCount(g.groupId());
            totalMembers += members;
            totalCapacity += g.maxMembers();
        }
        double avgMembers = allGroups.isEmpty() ? 0 : (double) totalMembers / allGroups.size();
        double capacityUtilization = totalCapacity == 0 ? 0 : ((double) totalMembers / totalCapacity) * 100;
        java.util.List<String[]> groupRows = new java.util.ArrayList<>();
        groupRows.add(new String[]{"Total Study Groups", String.valueOf(allGroups.size())});
        groupRows.add(new String[]{"Total Active Study Groups", String.valueOf(activeGroups)});
        groupRows.add(new String[]{"Inactive Groups", String.valueOf(inactiveGroups)});
        groupRows.add(new String[]{"Archived Groups", String.valueOf(archivedGroups)});
        groupRows.add(new String[]{"Average Members per Group", String.format("%.1f", avgMembers)});
        groupRows.add(new String[]{"Total Active Memberships", String.valueOf(totalMembers)});
        groupRows.add(new String[]{"Capacity Utilization", String.format("%.1f", capacityUtilization) + "%"});
        sections.add(new com.student_study_group_finder.report.ReportExporter.Section("GROUP STATISTICS", groupRows));

        java.util.Map<String, Integer> subjectMap = new java.util.TreeMap<>();
        for (StudyGroup g : allGroups) {
            if ("ACTIVE".equalsIgnoreCase(g.status())) {
                subjectMap.merge(g.subject(), 1, Integer::sum);
            }
        }
        java.util.List<String[]> subjectRows = new java.util.ArrayList<>();
        if (subjectMap.isEmpty()) {
            subjectRows.add(new String[]{"(no active groups)", ""});
        }
        for (java.util.Map.Entry<String, Integer> entry : subjectMap.entrySet()) {
            subjectRows.add(new String[]{entry.getKey(), String.valueOf(entry.getValue())});
        }
        sections.add(new com.student_study_group_finder.report.ReportExporter.Section("ACTIVE GROUPS PER SUBJECT", subjectRows));

        List<StudySession> allSessions = sessionDao.getAllSessions();
        long scheduled = allSessions.stream().filter(s -> "SCHEDULED".equalsIgnoreCase(s.status())).count();
        long inProgress = allSessions.stream().filter(s -> "IN_PROGRESS".equalsIgnoreCase(s.status())).count();
        long completed = allSessions.stream().filter(s -> "COMPLETED".equalsIgnoreCase(s.status())).count();
        long cancelled = allSessions.stream().filter(s -> "CANCELLED".equalsIgnoreCase(s.status())).count();
        java.util.List<String[]> sessionRows = new java.util.ArrayList<>();
        sessionRows.add(new String[]{"Total Scheduled Study Sessions", String.valueOf(allSessions.size())});
        sessionRows.add(new String[]{"Scheduled", String.valueOf(scheduled)});
        sessionRows.add(new String[]{"In Progress", String.valueOf(inProgress)});
        sessionRows.add(new String[]{"Total Completed Study Sessions", String.valueOf(completed)});
        sessionRows.add(new String[]{"Cancelled", String.valueOf(cancelled)});
        sections.add(new com.student_study_group_finder.report.ReportExporter.Section("SESSION STATISTICS", sessionRows));

        List<ActivityLogDao.ActionCount> summary = activityLogDao.getActionSummary();
        java.util.List<String[]> activityRows = new java.util.ArrayList<>();
        if (summary.isEmpty()) {
            activityRows.add(new String[]{"(no activity recorded)", ""});
        }
        for (ActivityLogDao.ActionCount ac : summary) {
            activityRows.add(new String[]{ac.action(), String.valueOf(ac.count())});
        }
        sections.add(new com.student_study_group_finder.report.ReportExporter.Section("ACTIVITY SUMMARY", activityRows));

        return sections;
    }

    /** Prints the full Study Group Activity Report to the console. */
    private void viewFullReport() {
        System.out.println("\n--- STUDY GROUP ACTIVITY REPORT ---");
        System.out.println("Generated: " + java.time.LocalDateTime.now());
        for (com.student_study_group_finder.report.ReportExporter.Section section : buildActivityReport()) {
            System.out.println("\n" + section.title());
            for (String[] row : section.rows()) {
                System.out.printf("  %-32s : %s%n", row[0], row[1]);
            }
        }
    }

    /** Exports the Study Group Activity Report in CSV, PDF, or XLSX format. */
    private void exportActivityReport() {
        System.out.println("\n--- EXPORT STUDY GROUP ACTIVITY REPORT ---");
        System.out.println("Select export format:");
        System.out.println("[1] CSV");
        System.out.println("[2] PDF");
        System.out.println("[3] Excel (XLSX)");
        System.out.print("Choose (1-3, 0 to cancel): ");
        String formatChoice = scanner.nextLine().trim();

        String extension = switch (formatChoice) {
            case "1" -> ".csv";
            case "2" -> ".pdf";
            case "3" -> ".xlsx";
            default -> null;
        };

        if (extension == null) {
            System.out.println("Export cancelled.");
            return;
        }

        System.out.print("Enter file name (e.g., report" + extension + "): ");
        String filePath = scanner.nextLine().trim();

        if (filePath.isEmpty()) {
            System.out.println("Export cancelled.");
            return;
        }

        if (!filePath.toLowerCase().endsWith(extension)) {
            filePath += extension;
        }

        try {
            com.student_study_group_finder.report.ReportExporter.export(
                    buildActivityReport(), java.nio.file.Path.of(filePath));
            java.time.LocalDateTime exportTime = java.time.LocalDateTime.now();
            activityLogDao.logActivity(currentAdmin.studentId(), "EXPORT_REPORT",
                    "Exported activity report to " + filePath,
                    exportTime.toLocalDate().toString(), exportTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            System.out.println("\u2705 Report exported successfully to: " + filePath);
        } catch (java.io.IOException e) {
            System.out.println("\u274C Failed to export report: " + e.getMessage());
        }
    }

    // ==================== ACTIVITY LOGS ====================

    private void activityLogsMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- ACTIVITY LOGS ---");
            System.out.println("[1] View All Activity Logs");
            System.out.println("[2] Clear All Logs");
            System.out.println("[3] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> viewAllLogs();
                case "2" -> clearAllLogs();
                case "3" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    private void viewAllLogs() {
        System.out.println("\n--- ALL ACTIVITY LOGS ---");
        List<ActivityLog> logs = activityLogDao.getAllLogs();

        if (logs.isEmpty()) {
            System.out.println("No activity logs found.");
            return;
        }

        printLogTable(logs);
    }

    private void clearAllLogs() {
        System.out.print("⚠️  This will delete ALL activity logs. Type CLEAR to confirm: ");
        String confirm = scanner.nextLine().trim();
        if (!"CLEAR".equalsIgnoreCase(confirm)) {
            System.out.println("Cancelled.");
            return;
        }

        boolean success = activityLogDao.clearAllLogs();
        if (success) {
            System.out.println("✅ All activity logs cleared.");
        } else {
            System.out.println("❌ Failed to clear logs.");
        }
    }

    // ==================== HELPER METHODS ====================

    /** Renders a formatted table of users with aligned columns. */
    private void printUserTable(List<Student> users) {
        // Layout help: "%-13s" = text column 13 wide ("%-5d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+-------+---------------+--------------+----------------+----------+");
        System.out.printf("| %-5s | %-13s | %-12s | %-14s | %-8s |\n", "ID", "Name", "Email", "Role", "Status");
        System.out.println("+-------+---------------+--------------+----------------+----------+");

        for (Student user : users) {
            String name = truncate(user.fullName(), 13);
            String email = truncate(user.email(), 12);
            String role = truncate(user.role(), 14);
            String status = truncate(user.accountStatus(), 8);
            System.out.printf("| %-5d | %-13s | %-12s | %-14s | %-8s |\n",
                    user.studentId(), name, email, role, status);
        }

        System.out.println("+-------+---------------+--------------+----------------+----------+");
        System.out.println("Total: " + users.size() + " user(s)");
    }

    /** Renders a formatted table of study groups. */
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

    /** Renders a formatted table of activity logs. */
    private void printLogTable(List<ActivityLog> logs) {
        // Layout help: "%-20s" = text column 20 wide ("%-6d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+--------+----------+------------------+----------------------+------------+------------+");
        System.out.printf("| %-6s | %-8s | %-16s | %-20s | %-10s | %-10s |\n",
                "Log ID", "User ID", "Action", "Details", "Date", "Time");
        System.out.println("+--------+----------+------------------+----------------------+------------+------------+");

        for (ActivityLog log : logs) {
            String details = log.details() != null ? truncate(log.details(), 20) : "N/A";
            String logDate = truncate(log.logDate(), 10);
            String logTime = truncate(log.logTime(), 10);
            System.out.printf("| %-6d | %-8d | %-16s | %-20s | %-10s | %-10s |\n",
                    log.logId(), log.studentId(), truncate(log.action(), 14), details, logDate, logTime);
        }
        System.out.println("+--------+----------+------------------+----------------------+------------+------------+");
        System.out.println("Total: " + logs.size() + " log(s)");
    }

    /** Truncates a string to the specified length and appends '...' if truncated. */
    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
}
