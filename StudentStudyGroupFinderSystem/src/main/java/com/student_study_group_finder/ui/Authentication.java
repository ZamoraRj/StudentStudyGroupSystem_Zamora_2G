package com.student_study_group_finder.ui;

import com.student_study_group_finder.dao.*;
import com.student_study_group_finder.model.Student;

import java.util.Scanner;
import java.util.Optional;

public class Authentication {
    private final StudentDao studentDao;
    private final StudyGroupDao studyGroupDao;
    private final GroupMembershipDao membershipDao;
    private final StudySessionDao sessionDao;
    private final ActivityLogDao activityLogDao;
    private final Scanner scanner;

    public Authentication(StudentDao studentDao, StudyGroupDao studyGroupDao,
                           GroupMembershipDao membershipDao, StudySessionDao sessionDao,
                           ActivityLogDao activityLogDao, Scanner scanner) {
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
            System.out.println("   STUDENT STUDY GROUP FINDER SYSTEM     ");
            System.out.println("=========================================");
            System.out.println("[1] Login");
            System.out.println("[2] Register New Account");
            System.out.println("[3] Exit Application");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> handleLogin();
                case "2" -> handleRegistration();
                case "3" -> {
                    System.out.println("Exiting application. Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid selection. Please enter 1, 2, or 3.");
            }
        }
    }

    private void handleRegistration() {
        System.out.println("\n--- NEW ACCOUNT REGISTRATION ---");
        System.out.print("Enter Full Name: ");
        String fullName = scanner.nextLine().trim();

        System.out.print("Enter Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Enter Course (or leave blank): ");
        String course = scanner.nextLine().trim();
        if (course.isEmpty()) {
            course = null;
        }

        System.out.print("Enter Year Level (or leave blank): ");
        String yearLevelStr = scanner.nextLine().trim();
        Integer yearLevel = null;
        if (!yearLevelStr.isEmpty()) {
            try {
                yearLevel = Integer.parseInt(yearLevelStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid year level. Skipping.");
            }
        }

        System.out.println("\nSelect Your System Role:");
        System.out.println("1. STUDENT");
        System.out.println("2. STUDY_GROUP_LEADER");
        System.out.println("3. ADMINISTRATOR");
        System.out.print("Choose role (1-3): ");
        String roleChoice = scanner.nextLine().trim();

        String role = switch (roleChoice) {
            case "1" -> "STUDENT";
            case "2" -> "STUDY_GROUP_LEADER";
            case "3" -> "ADMINISTRATOR";
            default -> null;
        };

        if (role == null) {
            System.out.println("Invalid role choice. Registration cancelled.");
            return;
        }

        // Check if email already exists
        if (studentDao.emailExists(email)) {
            System.out.println("\n❌ Registration failed. Email is already registered.");
            return;
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String dateRegistered = now.toLocalDate().toString();
        String timeRegistered = now.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();

        boolean success = studentDao.register(fullName, email, password, course, yearLevel, role, dateRegistered, timeRegistered);
        if (success) {
            System.out.println("\n✅ Account registered successfully!");
            System.out.println("Note: Account status is default to 'PENDING'. Admin approval required before login.");
            int newId = studentDao.findIdByEmail(email);
            if (newId != -1) {
                activityLogDao.logActivity(newId, "REGISTER",
                        "Registered new " + role + " account", dateRegistered, timeRegistered);
            }
        } else {
            System.out.println("\n❌ Registration failed. Please try again.");
        }
    }

    private void handleLogin() {
        System.out.println("\n--- USER LOGIN ---");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        System.out.print("Password: ");
        String password = scanner.nextLine().trim();

        Optional<Student> studentOpt = studentDao.login(email, password);

        if (studentOpt.isEmpty()) {
            System.out.println("\n❌ Login failed: Invalid email or password.");
            return;
        }

        Student student = studentOpt.get();

        if ("PENDING".equalsIgnoreCase(student.accountStatus())) {
            System.out.println("\n⚠️ Access Denied: Your account is still 'PENDING' approval.");
            return;
        }

        System.out.println("\n✅ Login successful! Welcome, " + student.fullName() + " (" + student.role() + ")");
        java.time.LocalDateTime loginTime = java.time.LocalDateTime.now();
        activityLogDao.logActivity(student.studentId(), "LOGIN",
                "User logged in", loginTime.toLocalDate().toString(), loginTime.toLocalTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());

        // Route to role-specific menu
        if ("ADMINISTRATOR".equalsIgnoreCase(student.role())) {
            AdminMenu adminMenu = new AdminMenu(student, studentDao, studyGroupDao, membershipDao, sessionDao, activityLogDao, scanner);
            adminMenu.start();
        } else if ("STUDY_GROUP_LEADER".equalsIgnoreCase(student.role())) {
            StudyGroupLeaderMenu leaderMenu = new StudyGroupLeaderMenu(student, studentDao, studyGroupDao, membershipDao, sessionDao, activityLogDao, scanner);
            leaderMenu.start();
        } else if ("STUDENT".equalsIgnoreCase(student.role())) {
            StudentMenu studentMenu = new StudentMenu(student, studentDao, studyGroupDao, membershipDao, sessionDao, activityLogDao, scanner);
            studentMenu.start();
        } else {
            System.out.println("Unknown role: " + student.role());
        }
    }
}