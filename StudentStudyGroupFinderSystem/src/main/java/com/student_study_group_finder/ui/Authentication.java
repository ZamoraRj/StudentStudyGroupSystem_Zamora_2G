package com.student_study_group_finder.ui;

import com.student_study_group_finder.dao.StudentDao;
import com.student_study_group_finder.model.Student;

import java.util.Scanner;
import java.util.Optional;

public class Authentication {
    private final StudentDao studentDao;
    private final Scanner scanner;

    public Authentication(StudentDao studentDao, Scanner scanner) {
        this.studentDao = studentDao;
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
        String dateRegistered = now.toString();
        String createdAt = now.toString();
        String updatedAt = now.toString();

        boolean success = studentDao.register(fullName, email, password, course, yearLevel, role, dateRegistered, createdAt, updatedAt);
        if (success) {
            System.out.println("\n✅ Account registered successfully!");
            System.out.println("Note: Account status is default to 'PENDING'. Admin approval required before login.");
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

    }
}