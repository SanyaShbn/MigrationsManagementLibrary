package parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class RollbackSqlParser {

    public List<String> generateRollbackScript(List<String> migrationSqlCommands) {
        List<String> rollbackCommands = new ArrayList<>();

        //Разделение команды в полученном массиве строк для последующей фильтрации
        List<String> splitCommands = new ArrayList<>();
        for (String command : migrationSqlCommands) {
            String[] lines = command.split("\r?\n");
            splitCommands.addAll(Arrays.asList(lines));
        }

        //Фильтрую, чтобы убрать комментарии, пустые строки и оставить непосредственно sql,
        //на основе которого формируем соответствующие rollback-файлики
        List<String> filteredCommands = splitCommands.stream()
                .filter(command -> !isComment(command) && !command.trim().isEmpty())
                .toList();

        for (String command : filteredCommands) {
            processCommand(command.trim(), rollbackCommands);
        }

        return rollbackCommands;
    }

    private void processCommand(String command, List<String> rollbackCommands) {
        switch (command.split(" ")[0].toUpperCase()) {
            case "CREATE" -> {
                if (command.contains("TABLE")) {
                    rollbackCommands.add(generateDropTableScript(command));
                } else if (command.contains("INDEX")) {
                    rollbackCommands.add(generateDropIndexScript(command));
                }
            }
            case "DROP" -> {
                if (command.contains("TABLE")) {
                    rollbackCommands.add(generateCreateTableScript(command));
                } else if (command.contains("INDEX")) {
                    rollbackCommands.add(generateCreateIndexScript(command));
                }
            }
            case "ALTER" -> {
                if (command.contains("TABLE")) {
                    rollbackCommands.add(generateAlterTableRollback(command));
                }
            }
            case "INSERT" -> rollbackCommands.add(generateDeleteFromTableScript(command));
            case "DELETE" -> rollbackCommands.add(generateInsertIntoTableScript(command));

            // Попробую обработать бОльшее разнообразие команд
            default -> {
            }
        }
    }

    private static boolean isComment(String command) {
        // Проверка на наличие многострочных комментариев
        command = command.replaceAll("/\\*.*?\\*/", "");
        // Проверка на наличие однострочных комментариев
        return command.trim().startsWith("--");
    }
    private String generateDropTableScript(String createTableCommand) {
        String tableName = extractTableName(createTableCommand);
        return "DROP TABLE IF EXISTS " + tableName + ";";
    }

    private String generateCreateTableScript(String dropTableCommand) {
        // Нужно будет реализовать сохранение структуры таблицы при ее создании
        // Это может быть сложно автоматизировать, поэтому рассмотрим это как пример
        return ""; // Возвращаем пустую строку, поскольку это сложно сделать автоматически
    }

    private String generateAlterTableRollback(String alterTableCommand) {
        if (alterTableCommand.contains("ADD COLUMN")) {
            return generateDropColumnScript(alterTableCommand);
        } else if (alterTableCommand.contains("DROP COLUMN")) {
            return generateAddColumnScript(alterTableCommand);
        }
        // Добавьте больше условий для других типов ALTER TABLE
        return "";
    }

    private String generateDropIndexScript(String createIndexCommand) {
        String indexName = extractIndexName(createIndexCommand);
        return "DROP INDEX IF EXISTS " + indexName + ";";
    }

    private String generateCreateIndexScript(String dropIndexCommand) {
        // Нужно будет реализовать сохранение структуры индекса при его создании
        // Это может быть сложно автоматизировать, поэтому рассмотрим это как пример
        return ""; // Возвращаем пустую строку, поскольку это сложно сделать автоматически
    }

    private String generateDeleteFromTableScript(String insertCommand) {
        // Нужно будет реализовать сохранение данных при их вставке
        // Это может быть сложно автоматизировать, поэтому рассмотрим это как пример
        return ""; // Возвращаем пустую строку, поскольку это сложно сделать автоматически
    }

    private String generateInsertIntoTableScript(String deleteCommand) {
        // Нужно будет реализовать сохранение данных при их удалении
        // Это может быть сложно автоматизировать, поэтому рассмотрим это как пример
        return ""; // Возвращаем пустую строку, поскольку это сложно сделать автоматически
    }

    private String generateDropColumnScript(String alterTableCommand) {
        String tableName = extractTableName(alterTableCommand);
        String columnName = extractColumnName(alterTableCommand, "ADD COLUMN");
        return "ALTER TABLE " + tableName + " DROP COLUMN " + columnName + ";";
    }

    private String generateAddColumnScript(String alterTableCommand) {
        String tableName = extractTableName(alterTableCommand);
        String columnDefinition = extractColumnDefinition(alterTableCommand, "DROP COLUMN");
        return "ALTER TABLE " + tableName + " ADD COLUMN " + columnDefinition + ";";
    }

    private String extractTableName(String sqlCommand) {
        String[] tokens = sqlCommand.split(" ");
        return tokens[2]; // Это пример, в реальном коде нужно обработать различные варианты синтаксиса
    }

    private String extractIndexName(String createIndexCommand) {
        String[] tokens = createIndexCommand.split(" ");
        return tokens[2]; // Это пример, в реальном коде нужно обработать различные варианты синтаксиса
    }

    private String extractColumnName(String alterTableCommand, String keyword) {
        int startIndex = alterTableCommand.indexOf(keyword) + keyword.length();
        String[] tokens = alterTableCommand.substring(startIndex).trim().split(" ");
        return tokens[0];
    }

    private String extractColumnDefinition(String alterTableCommand, String keyword) {
        int startIndex = alterTableCommand.indexOf(keyword) + keyword.length();
        return alterTableCommand.substring(startIndex).trim();
    }
}