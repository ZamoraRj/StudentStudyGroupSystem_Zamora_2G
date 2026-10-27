package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles the SQLite connection and creates the students table.
 */
public class DatabaseConnection {

    private static final String URL = "jdbc:sqlite:studygroupfinder.db";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() {
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
                    account_status  TEXT NOT NULL DEFAULT 'PENDING',
                    created_at      TEXT,
                    updated_at      TEXT
                )
                """;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            if (!hasColumn(stmt, "role")) {
                stmt.execute("""
                        ALTER TABLE students
                        ADD COLUMN role TEXT NOT NULL DEFAULT 'STUDENT'
                            CHECK(role IN ('STUDENT', 'STUDY_GROUP_LEADER', 'ADMINISTRATOR'))
                        """);
            }
        } catch (SQLException e) {
            System.out.println("Database setup failed: " + e.getMessage());
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
