package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
        migrate(conn);
    } catch (SQLException e) {
        throw new DatabaseException("Could not initialize the database schema", e);
    }
}

    /**
     * Adds columns that were introduced after the table was first created.
     *
     * Databases created by older versions of the app lack the metadata
     * columns (volumes, demographic, genres), so they are added here with
     * ALTER TABLE. Purely additive: existing rows and data are untouched.
     */
    private static void migrate(Connection conn) throws SQLException {
        var existingColumns = new java.util.HashSet<String>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(manga)")) {
            while (rs.next()) {
                existingColumns.add(rs.getString("name"));
            }
        }

        var newColumns = java.util.Map.of(
                "total_volumes", "INTEGER NOT NULL DEFAULT 0",
                "demographic", "TEXT",
                "genres", "TEXT",
                "metadata_synced", "INTEGER NOT NULL DEFAULT 0"
        );

        try (Statement stmt = conn.createStatement()) {
            for (var column : newColumns.entrySet()) {
                if (!existingColumns.contains(column.getKey())) {
                    stmt.execute("ALTER TABLE manga ADD COLUMN " + column.getKey() + " " + column.getValue());
                }
            }
        }
    }

}
