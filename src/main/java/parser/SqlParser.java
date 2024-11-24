package parser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlParser {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
            "(?i)(CREATE TABLE|INSERT INTO|DROP TABLE|UPDATE|ALTER TABLE)\\s+([a-zA-Z_][a-zA-Z0-9_]*)");

    public String extractTableName(String sql) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }
}
