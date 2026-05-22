package db;

/**
 * Custom unchecked exception used to represent database-related failures
 * in the application.
 *
 * This class wraps lower-level exceptions, such as SQLException, so the rest
 * of the application does not need to depend directly on JDBC-specific
 * exception types.
 *
 * For example, the repository layer can catch a SQLException and throw a
 * DatabaseException instead. The UI or service layer can then handle database
 * errors in one consistent way.
 */
public class DatabaseException extends RuntimeException {

    /**
     * Creates a database exception with a message and the original cause.
     *
     * The cause is usually the lower-level exception that triggered this
     * failure, such as a SQLException.
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a database exception with only a message.
     *
     * This is useful when the database operation technically succeeded,
     * but the application still considers the result an error, such as
     * updating zero rows because no manga with the given ID exists.
     */
    public DatabaseException(String message) {
        super(message);
    }
}
