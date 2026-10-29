package com.student_study_group_finder.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Ensures all required database tables exist automatically before the CLI menu starts.
 */
public class DatabaseInitializer {

    public static void initializeDatabase() {
        String sql = """
                CREATE TABLE IF NOT EXISTS students (
                    student_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name       TEXT NOT NULL,
                    email           TEXT NOT NULL UNIQUE,
                    password_hash        TEXT NOT NULL,
                    course          TEXT,
                    year_level      INTEGER,
                    role            TEXT NOT NULL DEFAULT 'STUDENT'
                        CHECK(role IN ('STUDENT', 'STUDY_GROUP_LEADER', 'ADMINISTRATOR')),
                    date_registered TEXT,
                    account_status  TEXT NOT NULL DEFAULT 'PENDING',
                    created_at      TEXT,
                    updated_at      TEXT
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);

            if (!hasColumn(stmt, "role")) {
                stmt.execute("""
                        ALTER TABLE students
                        ADD COLUMN role TEXT NOT NULL DEFAULT 'STUDENT'
                            CHECK(role IN ('STUDENT', 'STUDY_GROUP_LEADER', 'ADMINISTRATOR'))
                        """);
            }

            System.out.println("[Database Initializer] Database tables verified and initialized successfully.");

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Database initialization failed; the application cannot start.", e);
        }
    }

    private static boolean hasColumn(Statement stmt, String columnName) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(students)")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}