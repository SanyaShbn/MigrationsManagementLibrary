# MigrationsManagementLibrary

MigrationsManagementLibrary is a library for managing database migrations. It is designed to be used in any Java application and includes features for applying migrations, rolling back migrations, cherry picking migrations and checking the status of the database as well as generating CSV/JSON migration results reports.

## Installation

To use this library in your project, you need to download the JAR file from the GitHub repository and install it in your local Maven repository.

### Step 1: Download the JAR file

Download the JAR file from the `lib` directory in the GitHub repository:
[Download MigrationsManagementLibrary-1.0-SNAPSHOT.jar](https://github.com/SanyaShbn/MigrationsManagementLibrary/blob/master/lib/MigrationsManagementLibrary-1.0-SNAPSHOT.jar)

### Step 2: Install the JAR file in your local Maven repository

Open a terminal and run the following command:

```sh
mvn install:install-file -Dfile=path/to/your-jar-file/MigrationsManagementLibrary-1.0-SNAPSHOT.jar -DgroupId=com.example -DartifactId=SanyaShbn-migration-tool -Dversion=1.0-SNAPSHOT -Dpackaging=jar
```

Replace 'path/to/your-jar-file' with the actual path to the downloaded JAR file.

### Step 3: Add the dependency to your project's pom.xml
Add the following dependency to your pom.xml:
```
<dependency>
    <groupId>com.example</groupId>
    <artifactId>SanyaShbn-migration-tool</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
**Setting up Maven**

If Maven is not installed on your machine, follow these steps to install and configure Maven:

Download Maven:

Visit Apache Maven Download Page and download the binary zip archive.

Extract the archive:

Extract the downloaded archive to a directory of your choice (e.g., C:\Program Files\Maven).

Set environment variables:

Open the System Properties dialog (Win + Pause -> Advanced system settings -> Environment Variables).

Add a new system variable 'MAVEN_HOME' pointing to the Maven directory (e.g., C:\Program Files\Maven\apache-maven-3.8.4).

Add a new system variable 'M2_HOME' pointing to the same directory.

Edit the Path variable and add %MAVEN_HOME%\bin.

Verify the installation:

Open a new command prompt and run:
```
mvn -version
```
If you have done everything right, you should see the Maven version and other details.

### Usage

**Project environment**

It's preferred to place your migrations .sql files in the 'resources' directory of your project (but you can identify other external directories)

*Don't forget to specify your database as the data source*
*Attention! Notice that, for now, only PostgreSQL databases are supported by this library as a data source for applying migrations*

After your data source is connected you have to create 'application.properties' file in 'resources' and put there the following data:
```
db.url=your_db_url
db.username=your_db_username
db.password=your_db_password
```
The library also supports reading data from environment variables:
```
db.url=${DB_URL}
db.username=${DB_USERNAME}
db.password=${DB_PASSWORD}
```
*You also can identify some additional data: db.pool.size=${DB_POOL_SIZE}. The default value is 10 (pool size is used for managing connections to your database)*

**Applying Migrations**

To apply all available migrations, use the MigrationExecutor class:
```
import executor.MigrationExecutor;
import reader.MigrationFileReader;
import utils.MigrationManager;

public class MainApplication {

private final static String MIGRATIONS_DIRECTORY = "src/main/resources/db/migration"; //specify the directory for the migration files

    public static void main(String[] args) {
        MigrationFileReader migrationFileReader = new MigrationFileReader();
        MigrationManager migrationManager = new MigrationManager(migrationFileReader);
        MigrationExecutor migrationExecutor = new MigrationExecutor(migrationFileReader, migrationManager);
        
        // Apply all available migrations
        migrationExecutor.processMigrationFiles(MIGRATIONS_DIRECTORY);
    }
}
```
**Rolling Back Migrations**

To roll back to a specific version, use the RollbackExecutor:
```
import executor.RollbackExecutor;
import reader.MigrationFileReader;
import utils.MigrationManager;

public class MainApplication {

private final static String MIGRATIONS_DIRECTORY = "src/main/resources/db/rollback"; //specify the directory for the rollback files

    public static void main(String[] args) {
        MigrationFileReader migrationFileReader = new MigrationFileReader();
        MigrationManager migrationManager = new MigrationManager(migrationFileReader);
        RollbackExecutor rollbackExecutor = new RollbackExecutor(migrationFileReader, migrationManager);
        
        // Roll back to a specific version
        rollbackExecutor.rollbackToVersion(MIGRATIONS_DIRECTORY, 1);
    }
}
```
### Contributing
We welcome contributions! Please open an issue or submit a pull request on GitHub.

### CLI

Provided CLI is a command-line tool for managing database migrations based on the developped ;ibrary. It uses the library to apply migrations, roll back migrations, and check the status of the database.

## Installation

### Step 1: Download the JAR file

Download the JAR file from the `cli` directory in the GitHub repository:
[Download CLI-db-migrations-utility-1.0-SNAPSHOT.jar](https://github.com/SanyaShbn/MigrationsManagementLibrary/blob/master/cli/CLI-db-migrations-utility-1.0-SNAPSHOT.jar)

**Notice: it is necessary to complete step 1 and 2 of the 'Installation' guide part as well as set up Maven locally for proper work of this CLI utility**

### Running the CLI

To run the CLI, use the following command:
```
java -jar path_to_your_file/CLI-db-migrations-utility-1.0-SNAPSHOT.jar
```

You will be showed a menu and prompted to enter the database URL, username, password, and path to migration and rollback files. After that, you can choose an option to perform: 
'Migrate' - perform all the up to date migrations 
'Rollback' - execute a rollback to the chosen version of your database
'Status' - check status of the database (version and migrations with 'applied' status field value)
'Exit' - exit from utility app
