package reader;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class MigrationFileReader implements FileReader{
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

    @Override
    public List<String> readDbMigrationFile(File file) {
        try {
            return Files.readAllLines(Paths.get(file.getAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}