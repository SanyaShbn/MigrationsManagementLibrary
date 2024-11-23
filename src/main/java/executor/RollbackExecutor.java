package executor;

import lombok.extern.slf4j.Slf4j;
import parser.MigrationMetadata;
import parser.MigrationMetadataParser;
import reader.MigrationFileReader;
import utils.ConnectionManager;
import utils.MigrationManager;
import utils.SchemaHistoryUtil;
import utils.Validator;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

@Slf4j
public class RollbackExecutor implements Executor{

    // Для определения текущего состояния базы данных после отката до определенной версии идет обновление
    // статуса миграции
    private final static String UPDATE_MIGRATION_STATUS_SQL = """
            UPDATE schema_history_table
            SET status = 'rolled_back'
            WHERE version = ? AND script LIKE 'V%'
            """;

    private final static String UPDATE_CHERRY_PICKED_MIGRATION_STATUS_SQL = """
            UPDATE schema_history_table
            SET status = 'ignored'
            WHERE version = ? AND script LIKE 'V%'
            """;
    private final MigrationFileReader migrationFileReader;
    private final MigrationManager migrationManager;

    public RollbackExecutor(MigrationFileReader fileReader, MigrationManager migrationManager) {
        this.migrationFileReader = fileReader;
        this.migrationManager = migrationManager;
    }

    // Метод для выполнения отката к определенной версии
    public void rollbackToVersion(String directoryPath, int targetVersion) {
        List<File> rollbackFiles = migrationManager.findAndSortMigrationFiles(directoryPath);

        // Обратный порядок выполнения rollback-файлов
        Collections.reverse(rollbackFiles);

        try (Connection connection = ConnectionManager.get()) {
            connection.setAutoCommit(false);

            int currentVersion = migrationManager.getCurrentVersion(connection);
            if (targetVersion >= currentVersion) {
                throw new IllegalArgumentException("Target version must be less than current version");
            }

            for (File file : rollbackFiles) {
                Integer scriptVersion = migrationManager.extractVersionFromFilename(file);
                if (scriptVersion > targetVersion) {
                    List<String> sqlCommands = migrationFileReader.readDbMigrationFile(file);
                    if (!executeSqlWithCherryPick(connection, sqlCommands, file.getName(), scriptVersion, false)) {
                        connection.rollback();
                        log.error("Rollback failed, rolling back all changes");
                        return;
                    }
                }
            }

            connection.commit();
            log.info("Rollback executed successfully");
        } catch (SQLException e) {
            log.error("Error! Failed to rollback to version: ", e);
        }
    }

    // Метод для выполнения cherryPick rollback (один конкретный файл, а не все в обратном порядке
    // до определенной версии)
    public void cherryPickRollback(String directoryPath, int scriptVersion) {
        try (Connection connection = ConnectionManager.get()) {
            connection.setAutoCommit(false);

            File file = migrationManager.findRollbackFileByVersion(directoryPath, scriptVersion);
            if (file == null) {
                log.error("Rollback file for version {} not found.", scriptVersion);
                return;
            }

            List<String> sqlCommands = migrationFileReader.readDbMigrationFile(file);
            if (!executeSqlWithCherryPick(connection, sqlCommands, file.getName(), scriptVersion, true)) {
                connection.rollback();
                log.error("Cherrypick rollback failed, rolling back all changes.");
                return;
            }

            connection.commit();
            log.info("Cherrypick rollback for version {} executed successfully", scriptVersion);
        } catch (SQLException e) {
            log.error("Error! Failed to execute cherrypick rollback: ", e);
        }
    }

    // Обернул executeSql этим методом для правильного установления статуса rollback-а
    public boolean executeSqlWithCherryPick(Connection connection, List<String> sqlCommands,
                                            String script, Integer version, boolean isCherryPick) throws SQLException {
        boolean success = executeSql(connection, sqlCommands, script, version);
        if (success) {
            updateRolledBackMigrationStatus(connection, version, isCherryPick);
        }
        return success;
    }

    @Override
    public boolean executeSql(Connection connection, List<String> sqlCommands, String script, Integer version) {
        try {
            long startTime = System.currentTimeMillis();
            MigrationMetadata metadata = new MigrationMetadata();
            for (String sql : sqlCommands) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                    metadata = MigrationMetadataParser.parseMigrationMetadata(sql);
                }
            }
            long executionTime = System.currentTimeMillis() - startTime;
            SchemaHistoryUtil.updateSchemaHistoryTable(connection, version,
                    metadata.getDescription(),
                    script,
                    metadata.getInstalledBy(),
                    (int) executionTime,
                    true,
                    "applied");
            return true;
        } catch (SQLException e) {
            log.error("Execution of rollback script failed: ", e);
            return false;
        }
    }
    private void updateRolledBackMigrationStatus(Connection connection, Integer version, boolean isCherryPick)
            throws SQLException {
        String sql = isCherryPick ? UPDATE_CHERRY_PICKED_MIGRATION_STATUS_SQL : UPDATE_MIGRATION_STATUS_SQL;
        try (var preparedStatement = connection.prepareStatement(sql)) {
            Validator.checkNotNull(connection);
            preparedStatement.setInt(1, version);
            preparedStatement.executeUpdate();
        }
    }

}
