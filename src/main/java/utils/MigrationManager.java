package utils;

import lombok.extern.slf4j.Slf4j;
import reader.MigrationFileReader;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class MigrationManager {
    private final MigrationFileReader migrationFileReader;

    public MigrationManager(MigrationFileReader migrationFileReader) {
        this.migrationFileReader = migrationFileReader;
    }

    public List<File> findAndSortMigrationFiles(String directoryPath) {
        List<File> migrationFiles = migrationFileReader.findDbMigrationFiles(directoryPath);
        validateFileFormat(migrationFiles);
        List<File> mutableMigrationFiles = new ArrayList<>(migrationFiles);

        mutableMigrationFiles.sort(Comparator.comparing(this::extractVersionFromFilename));
        return mutableMigrationFiles;
    }

    private void validateFileFormat(List<File> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No migration files found in the path");
        }

        for (File file : files) {
            Validator.checkFileExists(file);
            Validator.checkFileFormat(file);
        }
    }

    //Получение текущей версии БД (пока просто наибольшее число в колонке version у schema_history_table)
    public Integer getCurrentVersion(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     """
                            SELECT MAX(version) FROM schema_history_table
                         """)){
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            log.error("Error retrieving current database version: ", e);
        }
        return null;
    }

    //Сравнение версии миграции и БД
    public boolean shouldApplyMigration(Integer currentVersion, Integer scriptVersion) {
        return currentVersion == null || scriptVersion > currentVersion;
    }

    //Версия указывается в названии файла, при помощи этого метода считываю ее, чтобы понять,
    // нужно ли применять данную миграцию еще раз
    public Integer extractVersionFromFilename(File file) {
        return Integer.valueOf(file.getName().split("__")[0].substring(1));
    }
}
