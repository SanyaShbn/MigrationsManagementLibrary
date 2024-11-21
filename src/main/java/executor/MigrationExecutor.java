package executor;

import lombok.extern.slf4j.Slf4j;
import parser.MigrationMetadata;
import parser.MigrationMetadataParser;
import reader.MigrationFileReader;
import utils.ConnectionManager;
import utils.MigrationManager;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Slf4j
public class MigrationExecutor {
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
    private final MigrationManager migrationManager;
    public MigrationExecutor(MigrationFileReader fileReader, MigrationManager migrationManager) {
        this.migrationFileReader = fileReader;
        this.migrationManager = migrationManager;
    }
    public void processMigrationFiles(String directoryPath) {
        List<File> migrationFiles = migrationManager.findAndSortMigrationFiles(directoryPath);
        try (Connection connection = ConnectionManager.get()) {
            createSchemaHistoryTable(connection);
            String currentVersion = migrationManager.getCurrentVersion(connection);
            for (File file : migrationFiles) {
                List<String> sqlCommands = migrationFileReader.readDbMigrationFile(file);
                String scriptVersion = migrationManager.extractVersionFromFilename(file);
                if (migrationManager.shouldApplyMigration(currentVersion, scriptVersion)) {
                    executeSql(connection, sqlCommands, file.getName(), scriptVersion);
                }
            }
        } catch (SQLException e) {
            log.error("Error! Failed to process migration files: ", e);
        }
    }

    private void executeSql(Connection connection, List<String> sqlCommands, String script, String version) {
        try {
            connection.setAutoCommit(false);
            boolean success = true;
            for (String sql : sqlCommands) {
                log.info("Executing SQL: {}", sql);
                MigrationMetadata metadata = MigrationMetadataParser.parseMigrationMetadata(sql);
                long startTime = System.currentTimeMillis();
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                    log.info("Executed SQL: {}", sql);
                } catch (SQLException e) {
                    success = false;
                    log.error("Error executing SQL: {}", sql, e);
                    connection.rollback();
                    break;
                }
                long executionTime = System.currentTimeMillis() - startTime;
                updateSchemaHistoryTable(
                        connection,
                        version,
                        metadata.getDescription() != null ? metadata.getDescription() : "No description",
                        script,
                        metadata.getInstalledBy() != null ? metadata.getInstalledBy() : "unknown",
                        (int) executionTime,
                        success);
            }
            if (success) {
                connection.commit();
            }
        } catch (SQLException e) {
            log.error("Migration execution failed: ", e);
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