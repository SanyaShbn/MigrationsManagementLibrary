package utils;

import java.io.File;
import java.sql.Connection;
import java.util.List;

//Класс для обработки null-значений и пустых значений
public class Validator {
    public static void checkNotNull(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("Error: Connection is closed");
        }
    }
    public static void checkNotNull(String str, String name) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    public static void checkNotNull(Integer str, String name) {
        if (str == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    public static void checkNotNull(List<String> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("Found no sql commands for executing migrations");
        }
    }

    //Проверка наличия нужных комментариев в .sql-файле для записи автора миграции
    // и описания миграции (комментария к ней) в schema_history_table
    public static void checkNotNullMigrationAuthorAndDescription(String installed_by, String description) {
        if (installed_by == null || installed_by.isEmpty()) {
            throw new IllegalArgumentException("Error processing migration files. You must identify the author" +
                    " of the migration in the comments");
        }
        if (description == null || description.isEmpty()) {
            throw new IllegalArgumentException("Error processing migration files. You must identify the description" +
                    " of the migration in the comments");
        }
    }

    public static void checkFileExists(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("Provided path is not a valid file: " +
                    (file != null ? file.getAbsolutePath() : "null"));
        }
    }

    //Проверка корректности указания имени файлов миграции и rollback-ов
    public static void checkMigrationFileFormat(File file) {
        checkFileExists(file);
        if (!file.getName().matches("V[0-9]+__.*\\.sql") &&
                !file.getName().matches("U[0-9]+__rollback_V[0-9]+__.*\\.sql")) {
            throw new IllegalArgumentException("Invalid migration file format: " + file.getName());
        }
    }

}


