import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import utils.PropertiesUtils;

public class PropertiesUtilsTest {

    @BeforeAll
    public static void setUp() {

        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:postgresql://localhost:5432/migrations-library-test");
        properties.setProperty("db.username", "user");
        properties.setProperty("db.password", "password");

        try (InputStream inputStream = PropertiesUtils.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetPropertyExists() {
        String value = PropertiesUtils.get("db.url");
        assertEquals("jdbc:postgresql://localhost:5432/migrations-library-test", value);
    }

    @Test
    public void testGetPropertyNotExists() {
        String value = PropertiesUtils.get("nonexistent.key");
        assertEquals(null, value);
    }
}
