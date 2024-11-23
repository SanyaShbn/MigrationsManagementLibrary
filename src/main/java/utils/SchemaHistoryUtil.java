package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

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
            INSERT INTO schema_history_table (version, description, script, checksum, installed_by, execution_time, success, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static void validateUpdateSchemaHistoryTableParams(Connection connection, Integer version, String description,
                                                               String script, String installed_by){
        Validator.checkNotNull(connection);
        Validator.checkNotNull(version, "Provided db version");
        Validator.checkNotNull(script, "Script");
        Validator.checkNotNullMigrationAuthorAndDescription(installed_by, description);
    }

    //Создание таблицы schema_history_table для отслеживания истории миграций
    public static void createSchemaHistoryTable(Connection connection) throws SQLException {
        Validator.checkNotNull(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_HISTORY_TABLE_SQL);
        }
    }

    //Внесение данных о новых миграциях в schema_history_table
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
}
