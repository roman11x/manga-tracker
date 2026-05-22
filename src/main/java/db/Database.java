package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles the low-level SQLite database setup for the application.
 *
 * This class is responsible for:
 * - creating database connections
 * - creating the manga table if it does not already exist
 * - creating indexes that help common queries run faster
 *
 * Other classes should use getConnection() when they need to interact
 * with the database.
 */

public class Database {

    private final static String DB_URL = "jdbc:sqlite:manga-tracker.db";

    /**
     * Creates and returns a new connection to the SQLite database.
     *
     * The caller is responsible for closing the connection when finished.
     *
     * @throws DatabaseException if the application cannot connect to the database
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            throw new DatabaseException("Could not connect to the database", e);
        }
    }

    /**
     * Initializes the database schema used by the application.
     *
     * This method is safe to call when the app starts because
     * CREATE TABLE IF NOT EXISTS and CREATE INDEX IF NOT EXISTS
     * will not recreate objects that already exist.
     */

    public static void initialize() {
        String createTableQuery = """
            CREATE TABLE IF NOT EXISTS manga (
                mal_id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                chapters_read INTEGER NOT NULL DEFAULT 0 CHECK (chapters_read >= 0),
                total_chapters INTEGER NOT NULL DEFAULT 0 CHECK (total_chapters >= 0),
                status TEXT NOT NULL DEFAULT 'PLAN_TO_READ',
                cover_path TEXT,
                added_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
            """;
        /*
         * Since this is a manga tracker, the application will often query
         * manga by status, such as PLAN_TO_READ, READING, COMPLETED, or DROPPED.
         * This index helps those status-based queries run faster.
         */
    String indexQuery = "CREATE INDEX IF NOT EXISTS idx_manga_status ON manga(status);";

        // try-with-resources automatically closes the connection and statement.
    try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
        stmt.execute(createTableQuery);
        stmt.execute(indexQuery);
    } catch (SQLException e) {
        throw new DatabaseException("Could not initialize the database schema", e);
    }
}

}
