package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** *
 * This class is used for CSV and JSON migrations results reports generation
 * */
@Slf4j
public class MigrationReportGenerator {

    private final static String SELECT_MIGRATION_HISTORY_SQL = """
            SELECT version, description, script, checksum, installed_on, installed_by, execution_time, success, status
            FROM schema_history_table
            """;

    /** *
     * Generating CSV-reports of migrations results
     *
     * @param filePath report file creation directory path
     * */
    public void generateCsvReport(String filePath) {
        try (Connection connection = ConnectionManager.get();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(SELECT_MIGRATION_HISTORY_SQL);
             FileWriter fileWriter = new FileWriter(filePath);
             CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT
                     .withHeader("Version", "Description", "Script", "Checksum", "Installed On", "Installed By",
                             "Execution Time", "Success", "Status"))) {

            while (resultSet.next()) {
                csvPrinter.printRecord(
                        resultSet.getInt("version"),
                        resultSet.getString("description"),
                        resultSet.getString("script"),
                        resultSet.getInt("checksum"),
                        resultSet.getTimestamp("installed_on"),
                        resultSet.getString("installed_by"),
                        resultSet.getInt("execution_time"),
                        resultSet.getBoolean("success"),
                        resultSet.getString("status")
                );
            }

            log.info("CSV report generated successfully: {}", filePath);
        } catch (SQLException | IOException e) {
            log.error("Error generating CSV report: ", e);
        }
    }

    /** *
     * Generating JSON-reports of migrations results
     *
     * @param filePath report file creation directory path
     * */
    public void generateJsonReport(String filePath) {
        try (Connection connection = ConnectionManager.get();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(SELECT_MIGRATION_HISTORY_SQL);
             FileWriter fileWriter = new FileWriter(filePath)) {

            List<MigrationHistory> historyList = new ArrayList<>();
            while (resultSet.next()) {
                MigrationHistory history = new MigrationHistory(
                        resultSet.getInt("version"),
                        resultSet.getString("description"),
                        resultSet.getString("script"),
                        resultSet.getInt("checksum"),
                        resultSet.getTimestamp("installed_on"),
                        resultSet.getString("installed_by"),
                        resultSet.getInt("execution_time"),
                        resultSet.getBoolean("success"),
                        resultSet.getString("status")
                );
                historyList.add(history);
            }

            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(fileWriter, historyList);

            log.info("JSON report generated successfully: {}", filePath);
        } catch (SQLException | IOException e) {
            log.error("Error generating JSON report: ", e);
        }
    }

    // Вспомогательный класс для представления истории миграций
    @AllArgsConstructor
    @Getter
    @Setter
    private static class MigrationHistory {
        private int version;
        private String description;
        private String script;
        private int checksum;
        private java.sql.Timestamp installedOn;
        private String installedBy;
        private int executionTime;
        private boolean success;
        private String status;
    }
}
