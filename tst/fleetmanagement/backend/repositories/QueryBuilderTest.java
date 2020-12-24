package fleetmanagement.backend.repositories;

import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;

public class QueryBuilderTest {

    private QueryBuilder tested;

    @Before
    public void setUp() {
        Connection connection = mock(Connection.class);
        tested = new QueryBuilder(connection);
    }

    @Test
    public void buildsCorrectInsertQuery() {
        String query = tested.insert("history")
                .insertField("timestamp", null)
                .insertField("device", 123)
                .toString();

        assertEquals("INSERT INTO history (timestamp,device) VALUES (?,?)", query);
    }

    @Test
    public void buildsCorrectDeleteQuery() {
        String query = tested.delete("history").where()
                .conditionEquals("timestamp", null)
                .conditionEquals("device", 123)
                .toString();

        assertEquals("DELETE FROM history WHERE timestamp is null  and device = ? ", query);
    }

    @Test
    public void buildsCorrectSelectAllQuery() {
        String query = tested.selectAll("history").where()
                .conditionEquals("timestamp", null)
                .conditionEquals("device", 123)
                .orderField("timestamp")
                .toString();

        assertEquals("SELECT * FROM history WHERE timestamp is null  and device = ?  ORDER BY timestamp", query);
    }


}