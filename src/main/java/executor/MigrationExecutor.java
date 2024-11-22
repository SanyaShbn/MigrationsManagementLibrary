package executor;

import lombok.extern.slf4j.Slf4j;
import parser.MigrationMetadata;
import parser.MigrationMetadataParser;
import reader.MigrationFileReader;
import utils.ConnectionManager;
import utils.MigrationManager;
import utils.Validator;

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
                            version INTEGER,
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

//Создаю таблицу schema_history_table единожды при загрузке класса
    static {
        try (Connection connection = ConnectionManager.get()) {
            createSchemaHistoryTable(connection);
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

    //Метод для последовательного выполнения sql-кода из всех созданных файлов миграции
    private boolean executeSql(Connection connection, List<String> sqlCommands, String script, Integer version) {
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
    private void updateSchemaHistoryTableParams(Connection connection, Integer version, String description,
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
            updateSchemaHistoryTable(connection, version,
                    metadata.getDescription(),
                    script,
                    metadata.getInstalledBy(),
                    (int) executionTime,
                    true);

            log.info("Migrating current schema to version {}", version);
            return true;
        }
    }

    //Создание таблицы schema_history_table для отслеживания истории миграций
    private static void createSchemaHistoryTable(Connection connection) throws SQLException {
        Validator.checkNotNull(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_HISTORY_TABLE_SQL);
        }
    }

    //Внесение данных о новых миграциях в schema_history_table
    private void updateSchemaHistoryTable(Connection connection, Integer version, String description,
                                          String script, String installed_by, int executionTime,
                                          boolean success) throws SQLException {
        updateSchemaHistoryTableParams(connection, version, description, script, installed_by);
        try (var preparedStatement = connection.prepareStatement(INSERT_INTO_HISTORY_TABLE_SQL)) {
            preparedStatement.setInt(1, version);
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