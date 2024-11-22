package reader;

import lombok.extern.slf4j.Slf4j;
import utils.Validator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class MigrationFileReader implements FileReader{

    //Метод для поиска .sql файлов в ресурсах
    @Override
    public List<File> findDbMigrationFiles(String path) {
        List<File> migrationFiles = new ArrayList<>();
        try {
            File directory = new File(path);
            if (!directory.exists() || !directory.isDirectory()) {
                throw new IOException("Directory not found: " + path);
            }
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".sql"));
            if (files != null) {
                migrationFiles.addAll(Arrays.asList(files));
            }
        }catch (IOException e) {
            log.error("Error analyzing directory for migrations: " + e.getMessage());
        }
        return migrationFiles;
    }

    //Чтение содержимого каждого файла
    @Override
    public List<String> readDbMigrationFile(File file) {
        Validator.checkFileExists(file);
        try {
            String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
            return Arrays.asList(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}