package reader;

import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MigrationFileReader implements FileReader{
    @Override
    @SneakyThrows
    public List<File> findDbMigrationFiles(String path) {
        File directory = new File(path);
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("Directory not found: " + path);
        }

        File[] files = directory.listFiles((dir, name) -> name.endsWith(".sql"));
        List<File> migrationFiles = new ArrayList<>();
        if (files != null) {
            migrationFiles.addAll(Arrays.asList(files));
        }
        return migrationFiles;
    }

    @Override
    @SneakyThrows
    public List<String> readDbMigrationFile(File file) {
        return Files.readAllLines(Paths.get(file.getAbsolutePath()));
    }
}