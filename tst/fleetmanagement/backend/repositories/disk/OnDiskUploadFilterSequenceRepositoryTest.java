package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.vehiclecommunication.upload.filter.ConditionType;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilter;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterSequence;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.config.Settings;
import fleetmanagement.test.TestFolderManager;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.UUID;

import static fleetmanagement.backend.vehiclecommunication.upload.filter.FilterType.AD_FILTER_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class OnDiskUploadFilterSequenceRepositoryTest {

    private static Group group;
    private static Vehicle vehicle;
    private static UploadFilter trueVehicleName;
    private static UploadFilter falseGroupName;
    private static UploadFilter falseFilename;
    private static TestScenario scenario;

    @Rule
    public TempFileRule tempDir = new TempFileRule();

    private OnDiskUploadFilterSequenceRepository tested;
    @Rule
    public TestFolderManager filterRootFolder = new TestFolderManager("Incoming");
    @Mock
    private Settings settings;

    @BeforeClass
    public static void beforeClass() {
        scenario = new TestScenario();
        group = new Group("Earth-Moon", null, false);
        scenario.groupRepository.insert(group);
        vehicle = new Vehicle("uic", null, "MoonTrain", "", ZonedDateTime.now(), group.id.toString(), false, 1);
        scenario.vehicleRepository.insert(vehicle);

        trueVehicleName = new UploadFilter("", "trueVehicleName", "trueVehicleName", "Disabled", "30");
        trueVehicleName.addCondition(ConditionType.VEHICLE_NAME, "Mo*in");
        falseGroupName = new UploadFilter("", "falseGroupName", "falseGroupName", "Disabled", "30");
        falseGroupName.addCondition(ConditionType.GROUP_NAME, "Mars-*");
        falseFilename = new UploadFilter("", "falseFilename", "falseFilename", "Disabled", "30");
        falseFilename.addCondition(ConditionType.FILE_NAME, "diag*is");

    }

    @Before
    public void beforeTest() throws Exception {
        MockitoAnnotations.initMocks(this);
        tempDir.clean();
        tested = new OnDiskUploadFilterSequenceRepository(tempDir, settings);
        filterRootFolder.addToDeleteList( "falseFilename");
        filterRootFolder.addToDeleteList( "falseGroupName");
        filterRootFolder.addToDeleteList( "trueVehicleName");

        when(settings.getIncomingFolderPath()).thenReturn(filterRootFolder.getRoot().getAbsolutePath());
    }

    @Test
    public void insert() {
        OnDiskUploadFilterSequenceRepository r1 = new OnDiskUploadFilterSequenceRepository(tempDir, settings);
        UploadFilterSequence filterSequence = new UploadFilterSequence(AD_FILTER_TYPE);
        filterSequence.addFilter(falseGroupName);
        filterSequence.addFilter(trueVehicleName);
        filterSequence.addFilter(falseFilename);
        r1.insert(filterSequence);

        tested.loadFromDisk();

        UploadFilterSequence byType = tested.findByType(AD_FILTER_TYPE);

        assertNotNull(byType);
        assertEquals(filterSequence.filters.size(), byType.filters.size());
    }

    @Test
    public void insertCreatesFiltersDirectories() {
        UploadFilterSequence filterSequence = new UploadFilterSequence(AD_FILTER_TYPE);
        filterSequence.addFilter(falseGroupName);
        filterSequence.addFilter(trueVehicleName);
        tested.insert(filterSequence);

        assertTrue(new File(filterRootFolder.getRoot(), "falseGroupName").exists());
        assertTrue(new File(filterRootFolder.getRoot(), "trueVehicleName").exists());
    }

    @Test
    public void update() {
        OnDiskUploadFilterSequenceRepository r1 = new OnDiskUploadFilterSequenceRepository(tempDir, settings);
        UploadFilterSequence filterSequence = new UploadFilterSequence(AD_FILTER_TYPE);
        filterSequence.addFilter(falseGroupName);
        filterSequence.addFilter(trueVehicleName);
        filterSequence.addFilter(falseFilename);
        r1.insert(filterSequence);

        UploadFilterSequence byType = r1.findByType(AD_FILTER_TYPE);

        r1.update(byType.id, s -> {
            s.filters.clear();
            s.filters.add(trueVehicleName);
        });

        tested.loadFromDisk();
        byType = tested.findByType(AD_FILTER_TYPE);

        assertEquals(1, byType.filters.size());
        assertTrue(byType.filters.contains(trueVehicleName));
    }


    @Test
    public void noException_WhenXmlFileIsEmpty() throws Exception {
        TempFile folder = tempDir.newFolder(UUID.randomUUID().toString());
        String xmlFile = tested.getXmlFile(folder).file().getName();
        folder.newFile(xmlFile);

        tested.loadFromDisk();
    }

}