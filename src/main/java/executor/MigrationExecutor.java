package executor;

import exception.LockException;
import lombok.extern.slf4j.Slf4j;
import parser.MigrationMetadata;
import parser.MigrationMetadataParser;
import parser.SqlParser;
import reader.MigrationFileReader;
import utils.ConnectionManager;
import utils.MigrationManager;
import utils.SchemaHistoryUtil;
import utils.Validator;

import java.io.File;
import java.sql.*;
import java.util.List;

@Slf4j
public class MigrationExecutor implements Executor {
    private final MigrationFileReader migrationFileReader;
    private final MigrationManager migrationManager;
    private final SqlParser sqlParser;

    static {
        try (Connection connection = ConnectionManager.get()) {
            SchemaHistoryUtil.createSchemaHistoryTable(connection);
        } catch (SQLException e) {
            log.error("Error! Failed to create Schema History table: ", e);
        }
    }

    public MigrationExecutor(MigrationFileReader fileReader, MigrationManager migrationManager) {
        this.migrationFileReader = fileReader;
        this.migrationManager = migrationManager;
        this.sqlParser = new SqlParser();
    }

    public void processMigrationFiles(String directoryPath) {
        List<File> migrationFiles = migrationManager.findAndSortMigrationFiles(directoryPath);
        try (Connection connection = ConnectionManager.get()) {
            connection.setAutoCommit(false);

            for (File file : migrationFiles) {
                List<String> sqlCommands = migrationFileReader.readDbMigrationFile(file);
                Integer scriptVersion = migrationManager.extractVersionFromFilename(file);
                Integer currentVersion = migrationManager.getCurrentVersion(connection);

                if (migrationManager.shouldApplyMigration(currentVersion, scriptVersion)) {
                    // Извлечение имен таблиц и проверка наличия колонки is_locked
                    for (String sql : sqlCommands) {
                        String tableName = sqlParser.extractTableName(sql);
                        if (tableName != null) {
                            ensureIsLockedColumn(connection, tableName);

                            // Проверка блокировки
                            if (isTableLocked(connection, tableName)) {
                                throw new LockException("Migration is locked. Another migration is in progress for table " + tableName);
                            }

                            // Установка блокировки
                            lockTable(connection, tableName, true);
                        }
                    }
                    try {
                        if (!executeSql(connection, sqlCommands, file.getName(), scriptVersion)) {
                            connection.rollback();
                            log.error("Migration failed, rolling back all changes.");
                            return;
                        }
                        connection.commit();
                        log.info("Migration executed successfully");
                    } catch (SQLException | IllegalArgumentException e) {
                        connection.rollback();
                        log.error("Error! Failed to process migration files: ", e);
                        throw e;
                    } finally {
                        // Разблокировка таблиц
                        for (String sql : sqlCommands) {
                            String tableName = sqlParser.extractTableName(sql);
                            if (tableName != null) {
                                lockTable(connection, tableName, false);
                            }
                        }
                    }
                }
            }
        } catch (SQLException | LockException e) {
            log.error("Error! Failed to process migration files: ", e);
        }
    }

    private void ensureIsLockedColumn(Connection connection, String tableName) throws SQLException {
        // Проверка существования таблицы
        if (!doesTableExist(connection, tableName)) {
            // Если таблица не существует, откладываем проверку до её создания
            return;
        }

        // Проверка наличия колонки is_locked
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, tableName, "is_locked")) {
            if (!columns.next()) {
                String alterTableQuery = "ALTER TABLE " + tableName + " ADD COLUMN is_locked BOOLEAN DEFAULT FALSE";
                try (PreparedStatement statement = connection.prepareStatement(alterTableQuery)) {
                    statement.executeUpdate();
                }
            }
        }
    }

    private boolean doesTableExist(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName, null)) {
            return tables.next();
        }
    }

    private boolean isTableLocked(Connection connection, String tableName) throws SQLException {
        if (!doesTableExist(connection, tableName)) {
            // Если таблица не существует, откладываем проверку до её создания
            return false;
        }
        String query = "SELECT is_locked FROM " + tableName + " WHERE is_locked = TRUE LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void lockTable(Connection connection, String tableName, boolean lock) throws SQLException {
        if (!doesTableExist(connection, tableName)) {
            // Если таблица не существует, откладываем проверку до её создания
            return;
        }
        String query = lock
                ? "UPDATE " + tableName + " SET is_locked = TRUE WHERE is_locked = FALSE"
                : "UPDATE " + tableName + " SET is_locked = FALSE";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.executeUpdate();
        }
    }

    @Override
    public boolean executeSql(Connection connection, List<String> sqlCommands, String script, Integer version) throws SQLException {
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
        Validator.checkNotNull(connection);
        Validator.checkNotNull(sqlCommands);
        Validator.checkNotNull(script, "Script");
        Validator.checkNotNull(version, "Provided db version");
    }

    private boolean executeSingleMigration(Connection connection, String sql, String script, Integer version) throws SQLException {
        MigrationMetadata metadata = MigrationMetadataParser.parseMigrationMetadata(sql);
        long startTime = System.currentTimeMillis();

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            log.info("Successfully applied 1 migration: {}", sql);

            long executionTime = System.currentTimeMillis() - startTime;
            SchemaHistoryUtil.updateSchemaHistoryTable(connection, version,
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