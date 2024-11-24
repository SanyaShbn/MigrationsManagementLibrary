package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import static utils.Validator.checkNotNull;
import static utils.Validator.checkNotNullMigrationAuthorAndDescription;

/** *
 * Utility class for managing Schema History table of migrations
 * */
public class SchemaHistoryUtil {
    private final static String CREATE_HISTORY_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS schema_history_table (
                            id SERIAL PRIMARY KEY,
                            version INTEGER,
                            description VARCHAR(200),
                            script VARCHAR(200) NOT NULL,
                            checksum INTEGER,
                            installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            installed_by VARCHAR(100) NOT NULL,
                            execution_time INTEGER,
                            success BOOLEAN NOT NULL,
                            status VARCHAR(50) DEFAULT 'applied'
                            );
            """;
    private final static String INSERT_INTO_HISTORY_TABLE_SQL = """
            INSERT INTO schema_history_table (version, description, script, checksum, installed_by, execution_time,
             success, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    /** *
     * Creating a schema_history_table to track migration history
     *
     * @param connection opened connection to the database
     * */
    public static void createSchemaHistoryTable(Connection connection) throws SQLException {
        checkNotNull(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_HISTORY_TABLE_SQL);
        }
    }

    /** *
     * Entering data about new migrations into schema_history_table
     *
     * @param connection opened connection to the database
     * @param version the version database is migrating to
     * @param description migration's description got from file comments
     * @param script file name
     * @param installedBy author of the migration
     * @param executionTime
     * @param success defines whether migration was successfully applied
     * @param status defines status of the migration (applied, rolled_back, ignored)
     * */
    public static void updateSchemaHistoryTable(Connection connection, Integer version, String description,
                                                String script, String installedBy, int executionTime,
                                                boolean success, String status) throws SQLException {
        validateUpdateSchemaHistoryTableParams(connection, version, description, script, installedBy);
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_INTO_HISTORY_TABLE_SQL)) {
            preparedStatement.setInt(1, version);
            preparedStatement.setString(2, description);
            preparedStatement.setString(3, script);
            preparedStatement.setInt(4, script.hashCode());
            preparedStatement.setString(5, installedBy);
            preparedStatement.setInt(6, executionTime);
            preparedStatement.setBoolean(7, success);
            preparedStatement.setString(8, status);
            preparedStatement.executeUpdate();
        }
    }
    private static void validateUpdateSchemaHistoryTableParams(Connection connection, Integer version, String description,
                                                               String script, String installed_by){
        checkNotNull(connection);
        checkNotNull(version, "Provided db version");
        checkNotNull(script, "Script");
        checkNotNullMigrationAuthorAndDescription(installed_by, description);
    }
}
