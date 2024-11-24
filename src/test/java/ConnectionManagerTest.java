import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import utils.ConnectionManager;

public class ConnectionManagerTest {

    @Test
    public void testGetConnection() {
        Connection connection = ConnectionManager.get();
        assertNotNull(connection);
    }

}
