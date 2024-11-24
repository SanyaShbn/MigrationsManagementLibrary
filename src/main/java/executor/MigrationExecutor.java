package executor;

import exception.LockException;
import lombok.extern.slf4j.Slf4j;
import parser.MigrationMetadata;
import parser.MigrationMetadataParser;
import reader.MigrationFileReader;
import utils.ConnectionManager;
import utils.MigrationManager;

import java.io.File;
import java.sql.*;
import java.util.List;

import static utils.MigrationLockUtil.*;
import static utils.SchemaHistoryUtil.createSchemaHistoryTable;
import static utils.SchemaHistoryUtil.updateSchemaHistoryTable;
import static utils.Validator.checkNotNull;

/** *
 * This class executes .sql files for db migrations
 * */
@Slf4j
public class MigrationExecutor implements Executor {
    private final MigrationFileReader migrationFileReader;
    private final MigrationManager migrationManager;

    static {
        try (Connection connection = ConnectionManager.get()) {
            createSchemaHistoryTable(connection);
            // Создание таблицы блокировки
            ensureMigrationLockTable(connection);
        } catch (SQLException e) {
            log.error("Error! Failed to create Schema History table: ", e);
        }
    }

    public MigrationExecutor(MigrationFileReader fileReader, MigrationManager migrationManager) {
        this.migrationFileReader = fileReader;
        this.migrationManager = migrationManager;
    }

    /** *
     * Processes migration files and applies them to the database.
     *
     * @param directoryPath the directory containing migration files
     * @throws LockException if a lock on migration cannot be acquired
     * */
    public void processMigrationFiles(String directoryPath) {
        List<File> migrationFiles = migrationManager.findAndSortMigrationFiles(directoryPath);
        try (Connection connection = ConnectionManager.get()) {

            checkLock(connection);

            connection.setAutoCommit(false);

            lockMigration(connection, true);

            int currentVersion = migrationManager.getCurrentVersion(connection);
            for (File file : migrationFiles) {
                List<String> sqlCommands = migrationFileReader.readDbMigrationFile(file);

                Integer scriptVersion = migrationManager.extractVersionFromFilename(file);

                if (migrationManager.shouldApplyMigration(currentVersion, scriptVersion)) {
                    if (!executeSql(connection, sqlCommands, file.getName(), scriptVersion)) {
                        connection.rollback();
                        lockMigration(connection, false);
                        log.error("Migration failed, rolling back all changes.");
                        return;
                    }
                }
            }
            lockMigration(connection, false);
            connection.commit();
            log.info("Migration executed successfully");
        } catch (SQLException | LockException e) {
            log.error("Error! Failed to process migration files: ", e);
            try (Connection connection = ConnectionManager.get()) {
                lockMigration(connection, false);
            } catch (SQLException ex) {
                log.error("Error! Failed to release lock: ", ex);
            }
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Executes SQL commands and updates the schema history table.
     * @param connection the database connection
     * @param sqlCommands the list of SQL commands to execute
     * @param script the name of the script file
     * @param version the version of the migration
     * @return true if the SQL execution was successful, false otherwise
     * */
    @Override
    public boolean executeSql(Connection connection, List<String> sqlCommands, String script, Integer version) {
        try {
            validateExecuteSqlParams(connection, sqlCommands, script, version);
            log.info("Started executing migration");

            for (String sql : sqlCommands) {
                if (!executeSingleMigration(connection, sql, script, version)) {
                    return false;
                }
            }

            return true;
        } catch (SQLException | IllegalArgumentException e) {
            log.error("Migration execution failed: ", e);
            return false;
        }
    }

    private void validateExecuteSqlParams(Connection connection, List<String> sqlCommands, String script,
                                          Integer version) {
        checkNotNull(connection);
        checkNotNull(sqlCommands);
        checkNotNull(script, "Script");
        checkNotNull(version, "Provided db version");
    }

    private boolean executeSingleMigration(Connection connection, String sql, String script,
                                           Integer version) throws SQLException {
        MigrationMetadata metadata = MigrationMetadataParser.parseMigrationMetadata(sql);
        long startTime = System.currentTimeMillis();

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            log.info("Successfully applied 1 migration: {}", sql);

            long executionTime = System.currentTimeMillis() - startTime;
            updateSchemaHistoryTable(connection, version,
                    metadata.getDescription(),
                    script,
                    metadata.getInstalledBy(),
                    (int) executionTime,
                    true,
                    "applied");

            log.info("Migrating current schema to version {}", version);
            return true;
        }
    }
}