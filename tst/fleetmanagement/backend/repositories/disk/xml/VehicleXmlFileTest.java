package fleetmanagement.backend.repositories.disk.xml;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleVersions;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.*;

public class VehicleXmlFileTest {

    @Rule
    public TempFileRule tempFolder = new TempFileRule();
    TestScenarioPrefilled scenario;

    @Before
    public void setup() {
        scenario = new TestScenarioPrefilled();
    }

    @Test
    public void loadsLegacyVersions() throws IOException {
        Vehicle vehicle = loadXml("legacy-database-files/vehicleVersions.xml");
        VehicleVersions versions = vehicle.versions;

        assertEquals(3, versions.getAll().size());
        assertEquals("2.0", versions.get(PackageType.Indis3MultimediaContent).version);
        assertEquals("V2015.05", versions.get(PackageType.DataSupply, 1).version);
        assertEquals("V2015.04", versions.get(PackageType.DataSupply, 2).version);
    }

    @Test
    public void loadsZero_ifVersionSlotIsNull() {
        VehicleVersions versions = scenario.vehicle1.versions;
        versions.add(new VehicleVersions.Versioned(PackageType.DataSupply, null, "dv.11"));
        Vehicle loaded = saveLoadXml(scenario.vehicle1);
        VehicleVersions.Versioned versioned = loaded.versions.get(PackageType.DataSupply);

        assertNotNull(versioned.slot);
        assertTrue(versioned.slot == 0);
    }

    @Test
    public void savesVersionsCorrectly() {
        VehicleVersions versions = scenario.vehicle1.versions;
        versions.add(new VehicleVersions.Versioned(PackageType.DataSupply, 1, "dv.11"));
        versions.add(new VehicleVersions.Versioned(PackageType.DataSupply, "dv.00"));
        versions.add(new VehicleVersions.Versioned(PackageType.OebbDigitalContent, "oebb.01"));
        versions.add(new VehicleVersions.Versioned(PackageType.XccEnnoSeatReservation, "xcc.01"));

        Vehicle loaded = saveLoadXml(scenario.vehicle1);
        versions = loaded.versions;

        assertEquals(4, versions.getAll().size());
        assertEquals("dv.11", versions.get(PackageType.DataSupply, 1).version);
        assertEquals("dv.00", versions.get(PackageType.DataSupply).version);
        assertEquals("oebb.01", versions.get(PackageType.OebbDigitalContent).version);
        assertEquals("xcc.01", versions.get(PackageType.XccEnnoSeatReservation).version);
    }

    @Test
    public void savesIpCorrectly() {
        scenario.vehicle1.ipAddress = "123.345.567.789";
        Vehicle loaded = saveLoadXml(scenario.vehicle1);
        assertEquals(scenario.vehicle1.ipAddress, loaded.ipAddress);
    }

    private Vehicle loadXml(String path) throws IOException {
        TempFile file = tempFolder.newFolder(UUID.randomUUID().toString()).append("vehicle.xml");
        FileUtils.copyFile(TestFiles.find(path), file);
        VehicleXmlFile tested = new VehicleXmlFile(file.getParentFile(), scenario.taskRepository);
        return tested.load();
    }

    private Vehicle saveLoadXml(Vehicle vehicle) {
        TempFile file = tempFolder.newFolder(UUID.randomUUID().toString());
        VehicleXmlFile xmlFile = new VehicleXmlFile(file, scenario.taskRepository);
        xmlFile.save(vehicle);
        return xmlFile.load();
    }

}