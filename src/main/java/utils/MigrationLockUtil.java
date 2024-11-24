package utils;

import exception.LockException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.extern.slf4j.Slf4j;

/** *
 * Utility class for managing migration locks
 * */
@Slf4j
public class MigrationLockUtil {

    private static final String LOCK_TABLE_NAME = "migration_lock";
    private static final String CREATE_LOCK_TABLE_SQL = "CREATE TABLE " + LOCK_TABLE_NAME + " (" +
            "id SERIAL PRIMARY KEY, " +
            "is_locked BOOLEAN NOT NULL DEFAULT FALSE)";

    /** *
     * Ensures the migration lock table exists
     *
     * @param connection the database connection
     * @throws LockException if trying to access locked migrations
     * */
    public static void checkLock(Connection connection) throws LockException {
        String lockQuery = "SELECT is_locked FROM migration_lock";
        try (PreparedStatement statement = connection.prepareStatement(lockQuery)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getBoolean("is_locked")) {
                    throw new LockException("Migration is locked. Another migration is in progress.");
                }
            }
        } catch (SQLException e) {
            throw new LockException("Failed to acquire lock. Another migration is in progress.");
        }
    }

    /** *
     * Ensures the migration lock table exists
     *
     * @param connection the database connection
     * @throws SQLException if a database access error occurs
     * */
    public static void ensureMigrationLockTable(Connection connection) throws SQLException {
        Validator.checkNotNull(connection);
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, LOCK_TABLE_NAME, null)) {
            if (!tables.next()) {
                try (PreparedStatement statement = connection.prepareStatement(CREATE_LOCK_TABLE_SQL)) {
                    statement.executeUpdate();
                }

                String insertInitialValueQuery = "INSERT INTO " + LOCK_TABLE_NAME + " (is_locked) VALUES (FALSE)";
                try (PreparedStatement statement = connection.prepareStatement(insertInitialValueQuery)) {
                    statement.executeUpdate();
                }
            }
        }
    }

    /**
     * Acquires or releases a migration lock
     * @param connection the database connection
     * @param lock true to acquire the lock, false to release the lock
     * @throws SQLException if a database access error occurs
     * @throws LockException if the lock cannot be acquired
     * */
    public static void lockMigration(Connection connection, boolean lock) throws SQLException {
        String sql = lock
                ? "UPDATE " + LOCK_TABLE_NAME + " SET is_locked = TRUE WHERE is_locked = FALSE"
                : "UPDATE " + LOCK_TABLE_NAME + " SET is_locked = FALSE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int updatedRows = statement.executeUpdate();
            if (lock && updatedRows == 0) {
                throw new LockException("Failed to acquire lock. Another migration is in progress.");
            }
        }
    }
}