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
import java.util.List;

@Slf4j
public class MigrationExecutor implements Executor{
    private final MigrationFileReader migrationFileReader;
    private final MigrationManager migrationManager;

//Создаю таблицу schema_history_table единожды при загрузке класса
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
    }

    //Непосредственно метод-обработчик файлов миграции из ресурсов
    public void processMigrationFiles(String directoryPath) {
        List<File> migrationFiles = migrationManager.findAndSortMigrationFiles(directoryPath);
        try (Connection connection = ConnectionManager.get()) {

            connection.setAutoCommit(false); //для того, чтобы всегда успевать сделать connection.rollback()
                                             // при необходимости

            int currentVersion = migrationManager.getCurrentVersion(connection); //получаю текущей версии БД
            for (File file : migrationFiles) {
                List<String> sqlCommands = migrationFileReader.readDbMigrationFile(file);

                //получаю версии файлов миграций для сравнения с версией бд
                Integer scriptVersion = migrationManager.extractVersionFromFilename(file);

                if (migrationManager.shouldApplyMigration(currentVersion, scriptVersion)) {
                    if(!executeSql(connection, sqlCommands, file.getName(), scriptVersion)){
                        connection.rollback(); //откат к предыдущему состоянию при возникновении исключений
                        log.error("Migration failed, rolling back all changes.");
                        return;
                    }
                }
            }
            connection.commit();
            log.info("Migration executed successfully");
        } catch (SQLException e) {
            log.error("Error! Failed to process migration files: ", e);
        }catch (IllegalArgumentException e){
            log.error(e.getMessage());
        }
    }

    @Override
    public boolean executeSql(Connection connection, List<String> sqlCommands, String script, Integer version) {
        try {
            validateExecuteSqlParams(connection, sqlCommands, script, version);
            log.info("Started executing migration");

            for (String sql : sqlCommands) {
                if(!executeSingleMigration(connection, sql, script, version)){
                    return false;
                }
            }
            return true;
        } catch (SQLException | IllegalArgumentException e) {
            log.error("Migration execution failed: ", e);
            return false;
        }
    }

    //Проверка на null при помощи утилитного класса Validator
    private void validateExecuteSqlParams(Connection connection, List<String> sqlCommands, String script,
                                          Integer version) {
        Validator.checkNotNull(connection);
        Validator.checkNotNull(sqlCommands);
        Validator.checkNotNull(script, "Script");
        Validator.checkNotNull(version, "Provided db version");
    }
    //Проверка на null при помощи утилитного класса Validator
    private void validateUpdateSchemaHistoryTableParams(Connection connection, Integer version, String description,
                                                String script, String installed_by){
        Validator.checkNotNull(connection);
        Validator.checkNotNull(version, "Provided db version");
        Validator.checkNotNull(script, "Script");
        Validator.checkNotNullMigrationAuthorAndDescription(installed_by, description);
    }

    //Выполнение sql-кода отдельного файла миграции
    private boolean executeSingleMigration(Connection connection, String sql, String script,
                                           Integer version) throws SQLException {
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