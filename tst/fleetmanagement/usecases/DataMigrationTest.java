package fleetmanagement.usecases;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.config.CommandsGenerator;
import fleetmanagement.config.Licence;
import fleetmanagement.config.LicenceImpl;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

public class DataMigrationTest {

    private DataMigration tested;
    private Licence licence;
    TempFile dataDir;
    TempFile oldDataDir;

    @Rule
    public TempFileRule tempDir = new TempFileRule();

    @Before
    public void setup() throws SocketException, UnknownHostException {
        dataDir = tempDir.newFolder("dataDir").newFolder("data");
        oldDataDir = tempDir.newFolder("oldDataDir").newFolder("data");
        licence = new LicenceImpl(dataDir);
        tested = new DataMigration(dataDir, licence);
    }

    @Test
    public void importLicence() throws Exception {
        String command = "{\n" +
                "\"date\": null,\n" +
                "\"vehicles\": 20,\n" +
                "\"geo\": true,\n" +
                "\"vehicleGeo\": true,\n" +
                "\"packages\": [\n" +
                "\"data_supply\", \n" +
                "\"remote_copystick\", \n" +
                "\"indis5_multimedia_content\",\n" +
                "],\n" +
                "\"diagnosis\": true,\n" +
                "\"reports\": true\n" +
                "}";

        LicenceImpl oldLicence = generateLicenceFile(command, oldDataDir);
        assertTrue(oldDataDir.append("licence.txt").exists());
        assertNotEquals(licence, oldLicence);

        tested.importLicence(oldDataDir.getAbsolutePath());

        assertEquals(licence, oldLicence);
        assertTrue(dataDir.append("licence.txt").exists());
    }

    @Test(expected = IOException.class)
    public void exceptionIfSourceDirEqualsDataDir() throws Exception {
        tested.importData(dataDir.getAbsolutePath());
    }

    @Test
    public void keepLogFilesUnchanged() throws Exception {
        File datalog = dataDir.newFolder(DataMigration.LOGS_DIRECTORY).newFile("data.log");
        FileUtils.writeStringToFile(datalog, datalog.getName());

        oldDataDir.newFolder(DataMigration.LOGS_DIRECTORY).newFile("data.log");

        tested.importData(oldDataDir.getAbsolutePath());

        assertEquals(FileUtils.readFileToString(datalog), datalog.getName() );
    }

    @Test
    public void importData() throws Exception {
        dataDir.newFolder("vehicles").newFile("vehicle.xml");
        dataDir.newFolder("packages").newFile("package.xml");

        oldDataDir.newFolder("tasks").newFile("task.xml");
        File vehicles = oldDataDir.newFolder("vehicles").newFile("vehicle.xml");
        FileUtils.writeStringToFile(vehicles, vehicles.getName());

        tested.importData(oldDataDir.getAbsolutePath());

        assertFalse(dataDir.append("packages/package.xml").exists() );
        assertTrue(dataDir.append("tasks/task.xml").exists() );
        assertTrue(dataDir.append("vehicles/vehicle.xml").exists() );
        assertEquals(FileUtils.readFileToString(dataDir.append("vehicles/vehicle.xml")), "vehicle.xml" );
    }

    LicenceImpl generateLicenceFile(String command, File directory) throws SocketException, UnknownHostException {
        LicenceImpl oldLicence = new LicenceImpl(directory);
        String encrypt = CommandsGenerator.encrypt(oldLicence.getInstallationSeed(), command);
        oldLicence.saveLicenceToFile(encrypt);
        return new LicenceImpl(directory);
    }

}