package ui;

import dao.StudentDao;
import model.Student;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console menus for the Student Study Group Finder System.
 */
public class MenuUI {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Scanner scanner = new Scanner(System.in);
    private final StudentDao studentDao = new StudentDao();

    public void start() {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("=== STUDENT STUDY GROUP FINDER SYSTEM ===");
            System.out.println("1. Register Account");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> registerScreen();
                case "2" -> loginScreen();
                case "3" -> {
                    System.out.println("Thank you for using the system. Goodbye and have a nice day!");
                    running = false;
                }
                default -> System.out.println("Invalid choice. Try again.");
            }
        }
        scanner.close();
    }

    private void registerScreen() {
        System.out.println("\n--- Register Account ---");
        System.out.print("Full name: ");
        String fullName = scanner.nextLine().trim();
        if (fullName.isEmpty()) {
            System.out.println("Full name cannot be empty.");
            return;
        }

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        if (email.isEmpty()) {
            System.out.println("Email cannot be empty.");
            return;
        }
        if (studentDao.emailExists(email)) {
            System.out.println("Email already registered.");
            return;
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();
        if (password.isEmpty()) {
            System.out.println("Password cannot be empty.");
            return;
        }

        System.out.println("Select role:");
        System.out.println("1. STUDENT");
        System.out.println("2. STUDY GROUP LEADER");
        System.out.println("3. ADMINISTRATOR");
        System.out.print("Choose: ");
        String role;
        switch (scanner.nextLine().trim()) {
            case "1" -> role = "STUDENT";
            case "2" -> role = "STUDY_GROUP_LEADER";
            case "3" -> role = "ADMINISTRATOR";
            default -> {
                System.out.println("Invalid role. Registration cancelled.");
                return;
            }
        }

        System.out.print("Course (optional): ");
        String courseInput = scanner.nextLine().trim();
        String course = courseInput.isEmpty() ? null : courseInput;

        System.out.print("Year level (optional): ");
        String yearLevelInput = scanner.nextLine().trim();
        Integer yearLevel = null;
        if (!yearLevelInput.isEmpty()) {
            try {
                yearLevel = Integer.valueOf(yearLevelInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid year level. Registration cancelled.");
                return;
            }
        }

        String today = LocalDate.now().toString();
        String now = LocalDateTime.now().format(TIMESTAMP_FORMAT);

        if (studentDao.register(fullName, email, password, course, yearLevel, role, today, now, now)) {
            System.out.println("Registration successful! Status: PENDING");
        }
    }

    private void loginScreen() {
        System.out.println("\n--- Login ---");
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        Optional<Student> result = studentDao.login(email, password);
        if (result.isEmpty()) {
            System.out.println("Invalid email or password.");
            return;
        }
        Student student = result.get();
        if ("PENDING".equals(student.accountStatus())) {
            System.out.println("Your account is still pending, wait for admins approval.");
            return;
        }
        userMenu(student);
    }

    private void userMenu(Student student) {
        System.out.println("\nWelcome, " + student.fullName() + "!");
        System.out.println("Role: " + student.role() + " | Status: " + student.accountStatus());

        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- User Menu ---");
            System.out.println("1. Logout");
            System.out.print("Choose: ");
            if (scanner.nextLine().trim().equals("1")) {
                System.out.println("Logged out successfully.");
                loggedIn = false;
            } else {
                System.out.println("Invalid choice. Try again.");
            }
        }
    }
}
