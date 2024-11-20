package processor;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.MigrationMetadata;
import parser.MigrationMetadataParser;
import reader.MigrationFileReader;
import utils.ConnectionManager;

public class MigrationFileProcessor {

    private final static String CREATE_HISTORY_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS schema_history_table (
                            id SERIAL PRIMARY KEY,
                            version VARCHAR(50) NOT NULL,
                            description VARCHAR(200),
                            script VARCHAR(200) NOT NULL,
                            checksum INTEGER,
                            installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            installed_by VARCHAR(100) NOT NULL,
                            execution_time INTEGER,
                            success BOOLEAN NOT NULL
                            );
            """;

    private final static String INSERT_INTO_HISTORY_TABLE_SQL = """
            INSERT INTO schema_history_table (version, description, script, checksum, installed_by, execution_time, success)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private final MigrationFileReader migrationFileReader;
    private static final Logger logger = LoggerFactory.getLogger(MigrationFileProcessor.class);

    public MigrationFileProcessor(MigrationFileReader fileReader) {
        this.migrationFileReader = fileReader;
    }

    public void processMigrationFiles(String directoryPath) {
        List<File> migrationFiles = migrationFileReader.findDbMigrationFiles(directoryPath);
        for (File file : migrationFiles) {
            List<String> sqlCommands = migrationFileReader.readDbMigrationFile(file);
            executeSql(sqlCommands, file.getName());
        }
    }

    private void executeSql(List<String> sqlCommands, String script) {

        try (Connection connection = ConnectionManager.get()) {
            createSchemaHistoryTable(connection);
            for (String sql : sqlCommands) {
                MigrationMetadata metadata = MigrationMetadataParser.parseMigrationMetadata(sql);
                long startTime = System.currentTimeMillis();
                boolean success = true;
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                    logger.info("Executed SQL: {}", sql);
                } catch (SQLException e) {
                    success = false;
                    logger.error("Error executing SQL: {}", sql, e);
                }
                long executionTime = System.currentTimeMillis() - startTime;
                updateSchemaHistoryTable(
                        connection,
                        metadata.getVersion() != null ? metadata.getVersion() : "unknown",
                        metadata.getDescription() != null ? metadata.getDescription() : "No description",
                        script,
                        metadata.getInstalledBy() != null ? metadata.getInstalledBy() : "unknown",
                        (int) executionTime,
                        success);
            }
        } catch (SQLException e) {
            logger.error("Error connecting to the database: ", e);
        }
    }

    private void createSchemaHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_HISTORY_TABLE_SQL);
        }
    }

    private void updateSchemaHistoryTable(Connection connection, String version, String description,
                                          String script, String installed_by, int executionTime, boolean success) throws SQLException {
        try (var preparedStatement = connection.prepareStatement(INSERT_INTO_HISTORY_TABLE_SQL)) {
            preparedStatement.setString(1, version);
            preparedStatement.setString(2, description);
            preparedStatement.setString(3, script);
            preparedStatement.setInt(4, script.hashCode());
            preparedStatement.setString(5, installed_by);
            preparedStatement.setInt(6, executionTime);
            preparedStatement.setBoolean(7, success);
            preparedStatement.executeUpdate();
        }
    }


}

