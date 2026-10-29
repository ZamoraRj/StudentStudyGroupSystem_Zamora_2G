package com.student_study_group_finder.ui;

import com.student_study_group_finder.dao.*;
import com.student_study_group_finder.model.*;

import java.util.List;
import java.util.Scanner;

/**
 * Study Group Leader menu: Create and manage study groups, update group information,
 * manage group members, create and manage study sessions, and view group activities.
 */
public class StudyGroupLeaderMenu {
    private final Student leader;
    private final StudentDao studentDao;
    private final StudyGroupDao studyGroupDao;
    private final GroupMembershipDao membershipDao;
    private final StudySessionDao sessionDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public StudyGroupLeaderMenu(Student leader, StudentDao studentDao, StudyGroupDao studyGroupDao,
                                 GroupMembershipDao membershipDao, StudySessionDao sessionDao,
                                 ActivityLogDao activityLogDao, Scanner scanner) {
        this.leader = leader;
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
            System.out.println("          STUDY GROUP LEADER MENU              ");
            System.out.println("=========================================");
            System.out.println("[1] Group Management");
            System.out.println("[2] Session Management");
            System.out.println("[3] Group Activities");
            System.out.println("[4] Profile & Account");
            System.out.println("[5] Logout");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> groupManagementMenu();
                case "2" -> manageSessions();
                case "3" -> viewActivities();
                case "4" -> viewProfile();
                case "5" -> {
                    java.time.LocalDateTime logoutTime = java.time.LocalDateTime.now();
                    activityLogDao.logActivity(leader.studentId(), "LOGOUT",
                            "User logged out", logoutTime.toLocalDate().toString(), logoutTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
                    System.out.println("Logging out. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please enter 1-5.");
            }
        }
    }

    /** Group-related actions: create, view, update, members, and removal. */
    private void groupManagementMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n=========================================");
            System.out.println("             GROUP MANAGEMENT            ");
            System.out.println("=========================================");
            System.out.println("[1] Create New Study Group");
            System.out.println("[2] View My Study Groups");
            System.out.println("[3] Update Group Information");
            System.out.println("[4] Manage Group Members");
            System.out.println("[5] Delete / Deactivate Study Group");
            System.out.println("[6] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> createGroup();
                case "2" -> viewMyGroups();
                case "3" -> updateGroupInfo();
                case "4" -> manageMembers();
                case "5" -> deleteOrDeactivateGroup();
                case "6" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    /** Create a new study group. */
    private void createGroup() {
        System.out.println("\n=========================================");
        System.out.println("          CREATE NEW STUDY GROUP         ");
        System.out.println("=========================================");
        System.out.print("Group Name: ");
        String groupName = scanner.nextLine().trim();
        if (groupName.isEmpty()) {
            System.out.println("Group name cannot be empty. Cancelled.");
            return;
        }

        System.out.print("Subject: ");
        String subject = scanner.nextLine().trim();
        if (subject.isEmpty()) {
            System.out.println("Subject cannot be empty. Cancelled.");
            return;
        }

        System.out.print("Description (or leave blank): ");
        String description = scanner.nextLine().trim();
        if (description.isEmpty()) {
            description = null;
        }

        System.out.print("Schedule (e.g., Mon/Wed 2-4PM or leave blank): ");
        String schedule = scanner.nextLine().trim();
        if (schedule.isEmpty()) {
            schedule = null;
        }

        System.out.print("Max Members (default 30): ");
        String maxMembersStr = scanner.nextLine().trim();
        int maxMembers = 30;
        if (!maxMembersStr.isEmpty()) {
            try {
                maxMembers = Integer.parseInt(maxMembersStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Using default (30).");
            }
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String nowDate = now.toLocalDate().toString();
        String nowTime = now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        boolean success = studyGroupDao.createGroup(groupName, description, subject, schedule, maxMembers,
                leader.studentId());

        if (success) {
            System.out.println("✅ Study group \"" + groupName + "\" created successfully!");
            // Auto-add creator as LEADER in the group
            int groupId = getLatestGroupId();
            membershipDao.addLeader(groupId, leader.studentId(), nowDate, nowTime);
            activityLogDao.logActivity(leader.studentId(), "CREATE_GROUP",
                    "Created group: " + groupName + " (group ID " + groupId + ")", nowDate, nowTime);
        } else {
            System.out.println("❌ Failed to create study group.");
        }
    }

    /** Delete or deactivate one of the leader's own groups. */
    private void deleteOrDeactivateGroup() {
        System.out.println("\n=========================================");
        System.out.println("     DELETE / DEACTIVATE STUDY GROUP     ");
        System.out.println("=========================================");
        List<StudyGroup> groups = studyGroupDao.getGroupsByLeader(leader.studentId());

        if (groups.isEmpty()) {
            System.out.println("You are not leading any study groups.");
            return;
        }

        System.out.println("Your groups:");
        for (StudyGroup group : groups) {
            System.out.println("  [" + group.groupId() + "] " + group.groupName() + " (" + group.status() + ")");
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

        StudyGroup group = studyGroupDao.getGroupById(groupId);
        if (group == null || group.createdBy() != leader.studentId()) {
            System.out.println("❌ You do not lead this group.");
            return;
        }

        System.out.println("[1] Deactivate (members kept, group hidden from search)");
        System.out.println("[2] Delete permanently (group and all its data are removed)");
        System.out.print("Choose (1-2, 0 to cancel): ");
        String action = scanner.nextLine().trim();

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String nowDate = now.toLocalDate().toString();
        String nowTime = now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();

        if (action.equals("1")) {
            boolean success = studyGroupDao.updateGroupStatus(groupId, "INACTIVE");
            if (success) {
                System.out.println("✅ Group deactivated.");
                activityLogDao.logActivity(leader.studentId(), "UPDATE_GROUP_STATUS",
                        "Deactivated group: " + group.groupName() + " (group ID " + groupId + ")", nowDate, nowTime);
            } else {
                System.out.println("❌ Failed to deactivate group.");
            }
        } else if (action.equals("2")) {
            System.out.print("⚠️  This will delete the group and all its data. Type DELETE to confirm: ");
            String confirm = scanner.nextLine().trim();
            if (!"DELETE".equalsIgnoreCase(confirm)) {
                System.out.println("Deletion cancelled.");
                return;
            }
            boolean success = studyGroupDao.deleteGroup(groupId);
            if (success) {
                System.out.println("✅ Group deleted successfully.");
                activityLogDao.logActivity(leader.studentId(), "DELETE_GROUP",
                        "Deleted group: " + group.groupName() + " (group ID " + groupId + ")", nowDate, nowTime);
            } else {
                System.out.println("❌ Failed to delete group.");
            }
        } else {
            System.out.println("Cancelled.");
        }
    }

    /** View all groups led by this user. */
    private void viewMyGroups() {
        System.out.println("\n=========================================");
        System.out.println("             MY STUDY GROUPS             ");
        System.out.println("=========================================");
        List<StudyGroup> groups = studyGroupDao.getGroupsByLeader(leader.studentId());

        if (groups.isEmpty()) {
            System.out.println("You are not leading any study groups.");
            return;
        }

        printGroupTable(groups);
    }

    /** Update information for one of the leader's groups. */
    private void updateGroupInfo() {
        System.out.println("\n=========================================");
        System.out.println("         UPDATE GROUP INFORMATION        ");
        System.out.println("=========================================");
        List<StudyGroup> groups = studyGroupDao.getGroupsByLeader(leader.studentId());

        if (groups.isEmpty()) {
            System.out.println("You are not leading any study groups.");
            return;
        }

        System.out.println("Your groups:");
        for (StudyGroup group : groups) {
            System.out.println("  [" + group.groupId() + "] " + group.groupName() + " (" + group.subject() + ")");
        }

        System.out.print("Enter Group ID to update (0 to cancel): ");
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
        if (group == null || group.createdBy() != leader.studentId()) {
            System.out.println("❌ You do not lead this group.");
            return;
        }

        System.out.println("\nCurrent info:");
        System.out.println("  Name: " + group.groupName());
        System.out.println("  Subject: " + group.subject());
        System.out.println("  Description: " + (group.description() != null ? group.description() : "N/A"));
        System.out.println("  Schedule: " + (group.schedule() != null ? group.schedule() : "N/A"));
        System.out.println("  Max Members: " + group.maxMembers());

        System.out.print("\nNew Group Name (or press Enter to keep): ");
        String newName = scanner.nextLine().trim();
        if (newName.isEmpty()) newName = group.groupName();

        System.out.print("New Subject (or press Enter to keep): ");
        String newSubject = scanner.nextLine().trim();
        if (newSubject.isEmpty()) newSubject = group.subject();

        System.out.print("New Description (or press Enter to keep): ");
        String newDesc = scanner.nextLine().trim();
        if (newDesc.isEmpty()) newDesc = group.description();

        System.out.print("New Schedule (or press Enter to keep): ");
        String newSchedule = scanner.nextLine().trim();
        if (newSchedule.isEmpty()) newSchedule = group.schedule();

        System.out.print("New Max Members (or press Enter to keep): ");
        String newMax = scanner.nextLine().trim();
        int newMaxMembers = group.maxMembers();
        if (!newMax.isEmpty()) {
            try {
                newMaxMembers = Integer.parseInt(newMax);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Keeping current value.");
            }
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String nowDate = now.toLocalDate().toString();
        String nowTime = now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        boolean success = studyGroupDao.updateGroup(groupId, newName, newDesc, newSubject, newSchedule, newMaxMembers);

        if (success) {
            System.out.println("✅ Group updated successfully!");
            activityLogDao.logActivity(leader.studentId(), "UPDATE_GROUP",
                    "Updated group: " + newName + " (group ID " + groupId + ")", nowDate, nowTime);
        } else {
            System.out.println("❌ Failed to update group.");
        }
    }

    /** Manage members of a group (view, remove, promote). */
    private void manageMembers() {
        System.out.println("\n=========================================");
        System.out.println("           MANAGE GROUP MEMBERS          ");
        System.out.println("=========================================");
        List<StudyGroup> groups = studyGroupDao.getGroupsByLeader(leader.studentId());

        if (groups.isEmpty()) {
            System.out.println("You are not leading any study groups.");
            return;
        }

        System.out.println("Your groups:");
        for (StudyGroup group : groups) {
            int count = studyGroupDao.getMemberCount(group.groupId());
            System.out.println("  [" + group.groupId() + "] " + group.groupName() + " (" + count + " members)");
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

        StudyGroup group = studyGroupDao.getGroupById(groupId);
        if (group == null || group.createdBy() != leader.studentId()) {
            System.out.println("❌ You do not lead this group.");
            return;
        }

        List<GroupMembership> members = membershipDao.getGroupMembers(groupId);
        if (members.isEmpty()) {
            System.out.println("No members in this group.");
            return;
        }

        System.out.println("\nMembers of \"" + group.groupName() + "\":");
        // Layout help: "%-13s" = text column 13 wide ("%-8d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+----------+---------------+--------------+------------+");
        System.out.printf("| %-8s | %-13s | %-12s | %-12s |\n", "User ID", "Name", "Role", "Joined");
        System.out.println("+----------+---------------+--------------+------------+");

        for (GroupMembership membership : members) {
            Student member = findStudentById(membership.studentId());
            String name = member != null ? truncate(member.fullName(), 13) : "Unknown";
            String joined = membership.joinDate() != null ? truncate(membership.joinDate(), 10) : "N/A";
            System.out.printf("| %-8d | %-13s | %-12s | %-12s |\n",
                    membership.studentId(), name, membership.role(), joined);
        }
        System.out.println("+----------+---------------+--------------+------------+");

        System.out.println("\nActions:");
        System.out.println("[1] Remove a Member");
        System.out.println("[2] Promote to Co-Leader");
        System.out.println("[3] Back");
        System.out.print("Select action: ");
        String action = scanner.nextLine().trim();

        if (action.equals("1")) {
            System.out.print("Enter User ID to remove (0 to cancel): ");
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
            if (userId == leader.studentId()) {
                System.out.println("❌ You cannot remove yourself.");
                return;
            }
            boolean removed = membershipDao.removeMember(groupId, userId);
            if (removed) {
                System.out.println("✅ Member removed successfully.");
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                activityLogDao.logActivity(leader.studentId(), "REMOVE_MEMBER",
                        "Removed user " + userId + " from group (group ID " + groupId + ")", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            } else {
                System.out.println("❌ Failed to remove member.");
            }
        } else if (action.equals("2")) {
            System.out.print("Enter User ID to promote (0 to cancel): ");
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
            boolean promoted = membershipDao.updateMemberRole(groupId, userId, "CO_LEADER");
            if (promoted) {
                System.out.println("✅ Member promoted to Co-Leader.");
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                activityLogDao.logActivity(leader.studentId(), "PROMOTE_MEMBER",
                        "Promoted user " + userId + " to CO_LEADER (group ID " + groupId + ")", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
            } else {
                System.out.println("❌ Failed to promote member.");
            }
        } else if (!action.equals("3")) {
            System.out.println("Invalid selection.");
        }
    }

    /** Create a new study session for one of the leader's groups. */
    private void createSession() {
        System.out.println("\n=========================================");
        System.out.println("           CREATE STUDY SESSION          ");
        System.out.println("=========================================");
        List<StudyGroup> groups = studyGroupDao.getGroupsByLeader(leader.studentId());

        if (groups.isEmpty()) {
            System.out.println("You are not leading any study groups.");
            return;
        }

        System.out.println("Your groups:");
        for (StudyGroup group : groups) {
            System.out.println("  [" + group.groupId() + "] " + group.groupName());
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

        System.out.print("Session Date (YYYY-MM-DD): ");
        String sessionDate = scanner.nextLine().trim();
        if (sessionDate.isEmpty()) {
            System.out.println("Session date cannot be empty. Cancelled.");
            return;
        }

        System.out.print("Start Time (HH:MM): ");
        String startTime = scanner.nextLine().trim();
        if (startTime.isEmpty()) {
            System.out.println("Start time cannot be empty. Cancelled.");
            return;
        }

        System.out.print("End Time (HH:MM): ");
        String endTime = scanner.nextLine().trim();
        if (endTime.isEmpty()) {
            System.out.println("End time cannot be empty. Cancelled.");
            return;
        }

        if (!isValidDate(sessionDate)) {
            System.out.println("Invalid date. Use YYYY-MM-DD. Cancelled.");
            return;
        }
        if (java.time.LocalDate.parse(sessionDate).isBefore(java.time.LocalDate.now())) {
            System.out.println("Session date cannot be in the past. Cancelled.");
            return;
        }
        if (!isValidTime(startTime) || !isValidTime(endTime)) {
            System.out.println("Invalid time. Use HH:MM. Cancelled.");
            return;
        }
        if (!java.time.LocalTime.parse(endTime).isAfter(java.time.LocalTime.parse(startTime))) {
            System.out.println("End time must be after start time. Cancelled.");
            return;
        }

        System.out.print("Location (or leave blank): ");
        String location = scanner.nextLine().trim();
        if (location.isEmpty()) location = null;

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String nowDate = now.toLocalDate().toString();
        String nowTime = now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        boolean success = sessionDao.createSession(groupId, sessionDate, startTime, endTime,
                location, leader.studentId());

        if (success) {
            System.out.println("✅ Study session on " + sessionDate + " created successfully!");
            activityLogDao.logActivity(leader.studentId(), "CREATE_SESSION",
                    "Created session on " + sessionDate + " (group ID " + groupId + ")", nowDate, nowTime);
        } else {
            System.out.println("❌ Failed to create session.");
        }
    }

    /** Combined: Create and manage (view, update, delete) study sessions. */
    private void manageSessions() {
        boolean running = true;
        while (running) {
            System.out.println("\n=========================================");
            System.out.println("          MANAGE STUDY SESSIONS          ");
            System.out.println("=========================================");
            System.out.println("[1] Create Study Session");
            System.out.println("[2] View & Edit Sessions");
            System.out.println("[3] Back");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> createSession();
                case "2" -> viewAndEditSessions();
                case "3" -> running = false;
                default -> System.out.println("Invalid selection.");
            }
        }
    }

    /** View sessions and perform edit actions (update, change status, delete). */
    private void viewAndEditSessions() {
        System.out.println("\n=========================================");
        System.out.println("           VIEW & EDIT SESSIONS          ");
        System.out.println("=========================================");
        List<StudyGroup> groups = studyGroupDao.getGroupsByLeader(leader.studentId());

        if (groups.isEmpty()) {
            System.out.println("You are not leading any study groups.");
            return;
        }

        System.out.println("Your groups:");
        for (StudyGroup group : groups) {
            System.out.println("  [" + group.groupId() + "] " + group.groupName());
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

        List<StudySession> sessions = sessionDao.getSessionsByGroup(groupId);
        if (sessions.isEmpty()) {
            System.out.println("No sessions in this group.");
            return;
        }

        System.out.println("\nSessions:");
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
            String loc = session.location() != null ? truncate(session.location(), 10) : "N/A";
            System.out.printf("| %-8d | %-10s | %-10s | %-10s | %-10s | %-10s |\n",
                    session.sessionId(), date, start, end, loc, session.status());
        }
        System.out.println("+----------+------------+------------+------------+------------+------------+");

        System.out.println("\nActions:");
        System.out.println("[1] Update Session");
        System.out.println("[2] Change Session Status");
        System.out.println("[3] Delete Session");
        System.out.println("[4] Back");
        System.out.print("Select action: ");
        String action = scanner.nextLine().trim();

        if (action.equals("1")) {
            updateSession(groupId);
        } else if (action.equals("2")) {
            changeSessionStatus(groupId);
        } else if (action.equals("3")) {
            deleteSession(groupId);
        } else if (!action.equals("4")) {
            System.out.println("Invalid selection.");
        }
    }

    /** Update a session's details. */
    private void updateSession(int groupId) {
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

        StudySession session = sessionDao.getSessionById(sessionId);
        if (session == null || session.groupId() != groupId) {
            System.out.println("❌ Session not found in this group.");
            return;
        }

        System.out.print("New Session Date (YYYY-MM-DD) (or press Enter to keep): ");
        String sDate = scanner.nextLine().trim();
        if (sDate.isEmpty()) sDate = session.sessionDate();

        System.out.print("New Start Time (HH:MM) (or press Enter to keep): ");
        String start = scanner.nextLine().trim();
        if (start.isEmpty()) start = session.startTime();

        System.out.print("New End Time (HH:MM) (or press Enter to keep): ");
        String end = scanner.nextLine().trim();
        if (end.isEmpty()) end = session.endTime();

        System.out.print("New Location (or press Enter to keep): ");
        String loc = scanner.nextLine().trim();
        if (loc.isEmpty()) loc = session.location();

        if (!isValidDate(sDate) || java.time.LocalDate.parse(sDate).isBefore(java.time.LocalDate.now())) {
            System.out.println("Invalid or past session date. Update cancelled.");
            return;
        }
        if (!isValidTime(start) || !isValidTime(end)
                || !java.time.LocalTime.parse(end).isAfter(java.time.LocalTime.parse(start))) {
            System.out.println("Invalid times, or end time is not after start time. Update cancelled.");
            return;
        }

        boolean success = sessionDao.updateSession(sessionId, sDate, start, end, loc);
        if (success) {
            System.out.println("✅ Session updated successfully!");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            activityLogDao.logActivity(leader.studentId(), "UPDATE_SESSION",
                    "Updated session (session ID " + sessionId + ", group ID " + groupId + ")", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to update session.");
        }
    }

    /** Change session status. */
    private void changeSessionStatus(int groupId) {
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
            System.out.println("✅ Session status changed to " + status + ".");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            activityLogDao.logActivity(leader.studentId(), "UPDATE_SESSION_STATUS",
                    "Status changed to " + status + " (session ID " + sessionId + ", group ID " + groupId + ")", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to update session status.");
        }
    }

    /** Delete a session. */
    private void deleteSession(int groupId) {
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
            System.out.println("✅ Session deleted successfully.");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            activityLogDao.logActivity(leader.studentId(), "DELETE_SESSION",
                    "Deleted session (session ID " + sessionId + ", group ID " + groupId + ")", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to delete session.");
        }
    }

    /** View group activities (activity logs). */
    private void viewActivities() {
        System.out.println("\n=========================================");
        System.out.println("             GROUP ACTIVITIES            ");
        System.out.println("=========================================");
        List<StudyGroup> groups = studyGroupDao.getGroupsByLeader(leader.studentId());

        if (groups.isEmpty()) {
            System.out.println("You are not leading any study groups.");
            return;
        }

        System.out.println("Your groups:");
        for (StudyGroup group : groups) {
            System.out.println("  [" + group.groupId() + "] " + group.groupName());
        }

        System.out.print("Enter Group ID (0 to see all activities): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            System.out.println("Cancelled.");
            return;
        }

        List<ActivityLog> logs;
        if (input.equals("0")) {
            logs = activityLogDao.getRecentLogs(50);
        } else {
            int groupId;
            try {
                groupId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input.");
                return;
            }
            logs = activityLogDao.getRecentLogs(50);
            // Filter by group-related actions (target is recorded in the details text)
            List<ActivityLog> filtered = new java.util.ArrayList<>();
            for (ActivityLog log : logs) {
                String groupMarker = "group ID " + groupId + ")";
                if (log.details() != null && log.details().contains(groupMarker)) {
                    filtered.add(log);
                }
            }
            logs = filtered;
        }

        if (logs.isEmpty()) {
            System.out.println("No activities found.");
            return;
        }

        // Layout help: "%-20s" = text column 20 wide ("%-6d" = number column).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("\n+--------+----------+------------------+----------------------+------------+------------+");
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
        System.out.println("Total: " + logs.size() + " activity(ies)");
    }

    /** View leader's profile. */
    private void viewProfile() {
        System.out.println("\n=========================================");
        System.out.println("                MY PROFILE               ");
        System.out.println("=========================================");
        String course = leader.course() != null ? truncate(leader.course(), 30) : "N/A";
        String yearLevel = leader.yearLevel() != null ? String.valueOf(leader.yearLevel()) : "N/A";
        // Layout help: "%-14s" = text column 14 wide ("%-30s" = 30 wide).
        // Each +---+ border piece needs width + 2 dashes. Change both together.
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Field", "Details");
        System.out.println("+----------------+--------------------------------+");
        System.out.printf("| %-14s | %-30s |\n", "Student ID", String.valueOf(leader.studentId()));
        System.out.printf("| %-14s | %-30s |\n", "Full Name", truncate(leader.fullName(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Email", truncate(leader.email(), 30));
        System.out.printf("| %-14s | %-30s |\n", "Role", leader.role());
        System.out.printf("| %-14s | %-30s |\n", "Course", course);
        System.out.printf("| %-14s | %-30s |\n", "Year Level", yearLevel);
        System.out.printf("| %-14s | %-30s |\n", "Account Status", leader.accountStatus());
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
        System.out.println("\n=========================================");
        System.out.println("             CHANGE PASSWORD             ");
        System.out.println("=========================================");
        System.out.print("Current Password: ");
        String current = scanner.nextLine().trim();
        if (!studentDao.checkPassword(leader.studentId(), current)) {
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

        if (studentDao.updatePassword(leader.studentId(), next)) {
            System.out.println("✅ Password changed successfully!");
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            activityLogDao.logActivity(leader.studentId(), "UPDATE_PASSWORD",
                    "Changed account password", now.toLocalDate().toString(), now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
        } else {
            System.out.println("❌ Failed to change password.");
        }
    }

    /** Update leader's profile information. */
    private void updateProfile() {
        System.out.println("\n=========================================");
        System.out.println("              UPDATE PROFILE             ");
        System.out.println("=========================================");
        System.out.print("New Full Name (or press Enter to keep): ");
        String newName = scanner.nextLine().trim();
        if (newName.isEmpty()) newName = leader.fullName();

        System.out.print("New Course (or press Enter to keep): ");
        String newCourse = scanner.nextLine().trim();
        if (newCourse.isEmpty()) newCourse = leader.course();

        System.out.print("New Year Level (or press Enter to keep): ");
        String newYearLevelStr = scanner.nextLine().trim();
        Integer newYearLevel = leader.yearLevel();
        if (!newYearLevelStr.isEmpty()) {
            try {
                newYearLevel = Integer.parseInt(newYearLevelStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Keeping current value.");
            }
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String nowDate = now.toLocalDate().toString();
        String nowTime = now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        boolean success = studentDao.updateProfile(leader.studentId(), newName, newCourse, newYearLevel);

        if (success) {
            System.out.println("✅ Profile updated successfully!");
            activityLogDao.logActivity(leader.studentId(), "UPDATE_PROFILE",
                    "Updated profile information", nowDate, nowTime);
        } else {
            System.out.println("❌ Failed to update profile.");
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

    /** Checks YYYY-MM-DD format for session dates. */
    private static boolean isValidDate(String value) {
        try {
            java.time.LocalDate.parse(value);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    /** Checks HH:MM format for session times. */
    private static boolean isValidTime(String value) {
        try {
            java.time.LocalTime.parse(value);
            return true;
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }

    /** Helper to find a student by ID. */
    private Student findStudentById(int id) {
        List<Student> all = studentDao.getAllUsers();
        for (Student s : all) {
            if (s.studentId() == id) return s;
        }
        return null;
    }

    /** Gets the latest created group ID (for auto-adding leader). */
    private int getLatestGroupId() {
        List<StudyGroup> groups = studyGroupDao.getGroupsByLeader(leader.studentId());
        if (groups.isEmpty()) return 0;
        int maxId = 0;
        for (StudyGroup g : groups) {
            if (g.groupId() > maxId) maxId = g.groupId();
        }
        return maxId;
    }

    /** Truncates a string to the specified length. */
    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
}
