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
                    password_hash   TEXT NOT NULL,
                    course          TEXT,
                    year_level      INTEGER,
                    role            TEXT NOT NULL DEFAULT 'STUDENT'
                        CHECK(role IN ('STUDENT', 'STUDY_GROUP_LEADER', 'ADMINISTRATOR')),
                    date_registered TEXT,
                    time_registered TEXT,
                    account_status  TEXT NOT NULL DEFAULT 'PENDING'
    
                )
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(sql);

            if (!hasColumn(stmt, "students", "role")) {
                stmt.execute("""
                        ALTER TABLE students
                        ADD COLUMN role TEXT NOT NULL DEFAULT 'STUDENT'
                            CHECK(role IN ('STUDENT', 'STUDY_GROUP_LEADER', 'ADMINISTRATOR'))
                        """);
            }

            // Add time_registered column if it doesn't exist
            if (!hasColumn(stmt, "students", "time_registered")) {
                stmt.execute("ALTER TABLE students ADD COLUMN time_registered TEXT");
            }

            // Study Groups table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS study_groups (
                        group_id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        group_name        TEXT NOT NULL,
                        description       TEXT,
                        subject           TEXT NOT NULL,
                        schedule          TEXT,
                        max_members       INTEGER NOT NULL DEFAULT 30,
                        created_by        INTEGER NOT NULL,
                        status            TEXT NOT NULL DEFAULT 'ACTIVE'
                            CHECK(status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
                        FOREIGN KEY (created_by) REFERENCES students(student_id)
                    )
                    """);

            // Add missing columns for existing databases
            if (!hasColumn(stmt, "study_groups", "schedule")) {
                stmt.execute("ALTER TABLE study_groups ADD COLUMN schedule TEXT");
            }

            // Migrate leader_id to created_by if old column exists
            if (hasColumn(stmt, "study_groups", "leader_id") && !hasColumn(stmt, "study_groups", "created_by")) {
                try {
                    stmt.execute("ALTER TABLE study_groups RENAME COLUMN leader_id TO created_by");
                } catch (SQLException e) {
                    // RENAME COLUMN not supported in older SQLite; ignore if fails
                }
            }

            // Group Memberships table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS group_memberships (
                        membership_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                        group_id          INTEGER NOT NULL,
                        student_id        INTEGER NOT NULL,
                        role              TEXT NOT NULL DEFAULT 'MEMBER'
                            CHECK(role IN ('MEMBER', 'LEADER', 'CO_LEADER')),
                        join_date         TEXT,
                        join_time         TEXT,
                        status            TEXT NOT NULL DEFAULT 'ACTIVE'
                            CHECK(status IN ('ACTIVE', 'LEFT', 'REMOVED')),
                        FOREIGN KEY (group_id) REFERENCES study_groups(group_id),
                        FOREIGN KEY (student_id) REFERENCES students(student_id),
                        UNIQUE(group_id, student_id)
                    )
                    """);

            // Add join_time column if it doesn't exist
            if (!hasColumn(stmt, "group_memberships", "join_time")) {
                stmt.execute("ALTER TABLE group_memberships ADD COLUMN join_time TEXT");
            }

            // Study Sessions table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS study_sessions (
                        session_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        group_id          INTEGER NOT NULL,
                        session_date      TEXT NOT NULL,
                        start_time        TEXT NOT NULL,
                        end_time          TEXT NOT NULL,
                        location          TEXT,
                        created_by        INTEGER NOT NULL,
                        status            TEXT NOT NULL DEFAULT 'SCHEDULED'
                            CHECK(status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
                        FOREIGN KEY (group_id) REFERENCES study_groups(group_id),
                        FOREIGN KEY (created_by) REFERENCES students(student_id)
                    )
                    """);

            // Migrate study_sessions: add session_date if missing
            if (!hasColumn(stmt, "study_sessions", "session_date")) {
                stmt.execute("ALTER TABLE study_sessions ADD COLUMN session_date TEXT NOT NULL DEFAULT '1970-01-01'");
            }

            // Activity Logs table
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS activity_logs (
                        log_id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        student_id        INTEGER NOT NULL,
                        action            TEXT NOT NULL,
                        details           TEXT,
                        log_date          TEXT NOT NULL,
                        log_time          TEXT NOT NULL,
                        ip_address        TEXT,
                        FOREIGN KEY (student_id) REFERENCES students(student_id)
                    )
                    """);

            // Migrate activity_logs: add log_date and log_time if missing (old schema used 'timestamp')
            if (!hasColumn(stmt, "activity_logs", "log_date")) {
                stmt.execute("ALTER TABLE activity_logs ADD COLUMN log_date TEXT NOT NULL DEFAULT '1970-01-01'");
            }
            if (!hasColumn(stmt, "activity_logs", "log_time")) {
                stmt.execute("ALTER TABLE activity_logs ADD COLUMN log_time TEXT NOT NULL DEFAULT '00:00:00'");
            }
            if (!hasColumn(stmt, "activity_logs", "ip_address")) {
                stmt.execute("ALTER TABLE activity_logs ADD COLUMN ip_address TEXT");
            }

            dropColumn(stmt, "students", "created_at");
            dropColumn(stmt, "students", "updated_at");
            dropColumn(stmt, "study_groups", "created_at");
            dropColumn(stmt, "study_groups", "updated_at");
            dropColumn(stmt, "study_sessions", "created_at");
            dropColumn(stmt, "study_sessions", "title");
            dropColumn(stmt, "study_sessions", "description");
            dropColumn(stmt, "study_sessions", "end_date");
            dropColumn(stmt, "activity_logs", "target_type");
            dropColumn(stmt, "activity_logs", "target_id");
            System.out.println("[Database Initializer] Database tables verified and initialized successfully.");

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Database initialization failed; the application cannot start.", e);
        }
    }

    private static void dropColumn(Statement stmt, String table, String column) {
        try {
            if (hasColumn(stmt, table, column)) {
                stmt.execute("ALTER TABLE " + table + " DROP COLUMN " + column);
            }
        } catch (SQLException e) {
            // Best effort cleanup for pre-existing databases; the column is simply left unused.
        }
    }

    private static boolean hasColumn(Statement stmt, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}