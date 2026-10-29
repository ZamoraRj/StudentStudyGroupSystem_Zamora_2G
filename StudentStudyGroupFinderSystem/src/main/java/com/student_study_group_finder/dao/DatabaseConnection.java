package com.student_study_group_finder.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles the SQLite connection and enforces foreign keys.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:studygroupfinder.db";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);

        // REQUIREMENT: Enable SQLite Foreign Key constraints on every connection initialization
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }

        return conn;
    }
}