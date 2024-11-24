package utils;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PropertiesUtils {
    private static final Properties PROPERTIES = new Properties();

    static {
        loadProperties();
    }
    private static void loadProperties() {
        try (var inputStream = PropertiesUtils.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (inputStream != null) {
                PROPERTIES.load(inputStream);

                for (String key : PROPERTIES.stringPropertyNames()) {
                    String value = PROPERTIES.getProperty(key);
                    if (value != null) {
                        value = resolveEnvVars(value);
                        PROPERTIES.setProperty(key, value);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private static String resolveEnvVars(String input) {
        if (null == input) {
            return null;
        }
        Pattern p = Pattern.compile("\\$\\{(\\w+)}|\\$(\\w+)");
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String envVarName = null == m.group(1) ? m.group(2) : m.group(1);
            String envVarValue = System.getenv(envVarName);
            m.appendReplacement(sb, null == envVarValue ? "" : envVarValue);
        }
        m.appendTail(sb);
        return sb.toString();
    }
    public static String get(String key){
        return PROPERTIES.getProperty(key);
    }
    private PropertiesUtils(){

    }
}