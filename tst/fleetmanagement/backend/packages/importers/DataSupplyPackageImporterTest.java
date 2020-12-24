package fleetmanagement.backend.packages.importers;


import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.test.TestScenario;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static fleetmanagement.backend.packages.importers.DataSupplyPackageImporter.*;
import static org.junit.Assert.*;

public class DataSupplyPackageImporterTest {

    private DataSupplyPackageImporter tested;

    private TestScenario scenario;

    @Before
    public void setup() {
        tested = new DataSupplyPackageImporter();
        scenario = new TestScenario();
    }

    @Rule
    public TempFileRule tempDir = new TempFileRule();

    @Test
    public void canImportTravisPackage() {
        int[] slotNumbers = {3, 5, 9};
        for (int slotNumber : slotNumbers) {
            TempFile packageFolder = tempDir.newFolder("packageFolder");
            String subFolderName = "datasupply" + (slotNumber == 1 ? "" : String.valueOf(slotNumber));
            packageFolder.newFolder(subFolderName);

            assertTrue(tested.canImportPackage("", packageFolder));
        }
    }

    @Test
    public void canImportIbisPackage() {
        int[] slotNumbers = {2, 4, 8};
        for (int slotNumber : slotNumbers) {
            TempFile packageFolder = tempDir.newFolder("packageFolder");
            String subFolderName = "IBIS" + (slotNumber == 1 ? "" : String.valueOf(slotNumber));
            packageFolder.newFolder(subFolderName);

            assertTrue(tested.canImportPackage("", packageFolder));
        }
    }

    @Test
    public void canNotImportPackageWithoutProperSubfolder() {
        TempFile packageFolder1 = tempDir.newFolder("packageFolder1");
        packageFolder1.newFolder("randomName");

        assertFalse(tested.canImportPackage("", packageFolder1));
    }

    @Test
    public void importsTravisPackageToProperSlot() throws IOException {
        int[] slotNumbers = {1, 3, 7};
        for (int slotNumber : slotNumbers) {
            TempFile packageFolder = tempDir.newFolder("packageFolder" + slotNumber);
            String subFolderName = "datasupply" + (slotNumber == 1 ? "" : String.valueOf(slotNumber));
            FileUtils.copyFileToDirectory(TestFiles.find("travis.db"), packageFolder.newFolder(subFolderName));
            Package imported = tested.importPackage("", packageFolder);

            assertNotNull(imported);
            assertEquals(Integer.valueOf(slotNumber), imported.slot);
        }
    }

    @Test
    public void findsSlotsByName() throws IOException {
        String folderName = "container";
        String folderName1 = "ibis";
        SimpleSlotFinder tested = new SimpleSlotFinder(folderName, folderName1);
        TempFile packageFolder = tempDir.newFolder("PackageFolder")
                .addFolder(folderName)
                .addFolder(folderName1)
                .addFolder("someFolder")
                .addFile("ibis.log");
        List<Slot> slots = tested.findSlots(packageFolder);

        assertEquals(2, slots.size());
        assertTrue(slots.stream().anyMatch(slot -> slot.directory.equals(folderName) && slot.slotNumber == 0));
        assertTrue(slots.stream().anyMatch(slot -> slot.directory.equals(folderName1) && slot.slotNumber == 0));
    }

    @Test
    public void findsSlotsByPattern() throws IOException {
        String folderPrefix = "ibis";
        DirectoryPatternSlotFinder tested = new DirectoryPatternSlotFinder(folderPrefix);
        TempFile packageFolder = tempDir.newFolder("PackageFolder")
                .addFolder("ibis")
                .addFolder("IBIS4")
                .addFolder("someFolder")
                .addFile("ibis.log");
        List<Slot> slots = tested.findSlots(packageFolder);

        assertEquals(2, slots.size());
        assertTrue(slots.stream().anyMatch(slot -> slot.directory.equals("ibis") && slot.slotNumber == 1));
        assertTrue(slots.stream().anyMatch(slot -> slot.directory.equals("IBIS4") && slot.slotNumber == 4));
    }

}