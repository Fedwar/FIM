package fleetmanagement;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.config.Licence;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class FleetManagementSmokeTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void startsAndStops() throws Exception {
        FleetManagement tested = new FleetManagement(folder.getRoot());
        tested.start();
        try {
            assertFrontendServerIsReachable();
            assertBackendServerIsReachable();
        } finally {
            tested.stop();
        }
    }

    @Test
    public void defaultLicence() throws Exception {
        FleetManagement tested = new FleetManagement(folder.getRoot());
        tested.start();

        try {
            Licence licence = tested.applicationContext.getBean(Licence.class);

            assertEquals(1, licence.getPackageTypes().size());
            assertEquals(PackageType.DataSupply, licence.getPackageTypes().iterator().next());
            assertNull(licence.getExpirationDate());
            assertEquals(5, licence.getMaximumVehicleCount());
            assertFalse(licence.isMapAvailable());
            assertFalse(licence.isDiagnosisInfoAvailable());
            assertFalse(licence.isAutoPackageSyncAvailable());
            assertFalse(licence.isOperationInfoAvailable());
            assertFalse(licence.isVehicleGeoAvailable());
        } finally {
            tested.stop();
        }
    }

    void storeProperty(String propertyName, String propertyValue) throws IOException {
        Properties properties = new Properties();
        properties.setProperty(propertyName, propertyValue);
        storeProperties(properties);
    }

    void storeProperties(Properties p) throws IOException {
        FileOutputStream fs = new FileOutputStream(new File(folder.getRoot(), AppConfig.CONFIG_FILE));
        p.store(fs, "Properties");
        fs.close();
    }

    private void assertFrontendServerIsReachable() throws Exception {
        URL url = new URL("http://localhost:" + AppConfig.FRONTEND_DEFAULT_PORT + "/login");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        assertEquals(200, connection.getResponseCode());
    }

    private void assertBackendServerIsReachable() throws Exception {
        URL url = new URL("http://localhost:" + AppConfig.BACKEND_DEFAULT_PORT + "/monitoring/health");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        assertEquals(200, connection.getResponseCode());
    }

}
