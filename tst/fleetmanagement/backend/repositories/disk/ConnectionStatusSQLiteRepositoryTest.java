package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TestObjectFactory;
import fleetmanagement.backend.reports.datasource.vehicles.ConnectionStatus;
import fleetmanagement.backend.vehicles.Vehicle;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionStatusSQLiteRepositoryTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private Vehicle vehicle = TestObjectFactory.createVehicle("v1");

    private ConnectionStatusSQLiteRepository repositorySpy;


    @Before
    public void setUp() throws Exception {
        temporaryFolder.newFolder(vehicle.id.toString());
        repositorySpy = new ConnectionStatusSQLiteRepository(temporaryFolder.getRoot());
    }

    @Test
    public void writeAndReadTest() {
        vehicle.lastSeen = ZonedDateTime.of(2020, 4, 20, 15, 30, 0, 0, ZoneId.systemDefault());
        repositorySpy.saveConnectionInfo(vehicle);

        Map<String, ConnectionStatus> res = repositorySpy.getVehicleHours("2020-04-20 15:00:00",
                "2020-04-20 16:00:00", vehicle);
        assertThat(res.size(), is(1));

        res = repositorySpy.getVehicleHours("2020-04-20 15:00:00",
                "2020-04-20 15:59:59", vehicle);
        assertThat(res.size(), is(1));
    }
}