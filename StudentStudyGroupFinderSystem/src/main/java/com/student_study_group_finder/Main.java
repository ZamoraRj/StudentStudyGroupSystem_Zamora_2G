package com.student_study_group_finder;

import com.student_study_group_finder.dao.DatabaseInitializer;
import com.student_study_group_finder.dao.StudentDao;
import com.student_study_group_finder.ui.Authentication;

import java.util.Scanner;

/**
 * Single entry point for the Student Study Group Finder System.
 * Execution flow: Initialize Database -> Instantiate DAOs & Utilities -> Delegate to Authentication.start().
 */
public class Main {
    public static void main(String[] args) {
        // Step 1: Initialize Database - ensure all required tables exist
        DatabaseInitializer.initializeDatabase();

        // Step 2: Instantiate shared DAOs & Utilities
        Scanner scanner = new Scanner(System.in);
        StudentDao studentDao = new StudentDao();

        // Step 3: Delegate execution to Authentication
        Authentication authentication = new Authentication(studentDao, scanner);
        authentication.start();
    }
}
