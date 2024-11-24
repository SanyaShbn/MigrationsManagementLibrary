import executor.MigrationExecutor;
import reader.MigrationFileReader;
import utils.MigrationManager;

public class MigrationLibraryRunner {
    private static final String MIGRATIONS_DIRECTORY_PATH = "src/main/resources/db/migration";

    public static void run() {
        MigrationFileReader migrationFileReader = new MigrationFileReader();
        MigrationManager migrationManager = new MigrationManager(migrationFileReader);
        MigrationExecutor migrationExecutor = new MigrationExecutor(migrationFileReader, migrationManager);

        Runnable migrationTask = () -> {
            try {
                migrationExecutor.processMigrationFiles(MIGRATIONS_DIRECTORY_PATH);
            } catch (Exception e) {
                System.out.println("Caught exception: " + e.getMessage());
            }
        };

        Thread thread1 = new Thread(migrationTask);
        Thread thread2 = new Thread(migrationTask);

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        run();
    }
}