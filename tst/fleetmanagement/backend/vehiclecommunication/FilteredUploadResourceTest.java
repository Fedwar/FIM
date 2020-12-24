package fleetmanagement.backend.vehiclecommunication;

import com.sun.jersey.core.header.FormDataContentDisposition;
import fleetmanagement.TempFileRule;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.vehiclecommunication.upload.filter.ConditionType;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilter;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterSequence;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.config.Settings;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static fleetmanagement.backend.vehiclecommunication.upload.filter.FilterType.AD_FILTER_TYPE;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class FilteredUploadResourceTest {

    private  Group group;
    private  Vehicle vehicle;

    private static UploadFilter trueVehicleFilter;
    private static UploadFilter trueFilenameFilter;
    private static UploadFilter trueGroupFilterWinSeparator;
    private static UploadFilter falseVehicleFilter;
    private  TestScenario scenario;
    private  FilteredUploadResource tested;

    private FormDataContentDisposition contentDisposition;
    private ByteArrayInputStream content;
    private UploadFilterSequence filterSequence;
    private String filename;
    @Rule
    public TempFileRule tempDir = new TempFileRule();
    @Mock
    public Settings settings;

    @BeforeClass
    public static void init() {
        trueVehicleFilter = new UploadFilter("trueVehicle",  "trueVehicleFilter/<group>/<vehicle>", "trueVehicle", "Disabled", "30");
        trueVehicleFilter.addCondition(ConditionType.VEHICLE_NAME, "Mo*in");

        trueFilenameFilter = new UploadFilter("trueVehicle",  "trueFilenameFilter/<vehicle>", "trueVehicle", "Disabled", "30");
        trueFilenameFilter.addCondition(ConditionType.FILE_NAME, "*.log");

        trueGroupFilterWinSeparator = new UploadFilter("trueVehicle",  "trueGroupFilterWinSeparator\\<group>\\<vehicle>", "trueVehicle", "Disabled", "30");
        trueVehicleFilter.addCondition(ConditionType.GROUP_NAME, "Ea*on");

        falseVehicleFilter = new UploadFilter("trueVehicle",  "<group>/<vehicle>", "trueVehicle", "Disabled", "30");
        falseVehicleFilter.addCondition(ConditionType.VEHICLE_NAME, "Spa*ta");
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        filename = "tttt2.log";

        content = new ByteArrayInputStream(new byte[]{1, 2, 3, 4, 5});
        contentDisposition = FormDataContentDisposition.name("file").fileName(filename).size(5).build();

        scenario = new TestScenario();
        when(settings.getIncomingFolderPath()).thenReturn(tempDir.getAbsolutePath());

        group = new Group("Earth-Moon", null, false);
        scenario.groupRepository.insert(group);
        vehicle = new Vehicle("uic",  null, "MoonTrain", "", ZonedDateTime.now(), group.id.toString(),false, 1);
        scenario.vehicleRepository.insert(vehicle);
        filterSequence = new UploadFilterSequence(AD_FILTER_TYPE);
        scenario.filterRepository.insert(filterSequence);

        tested = new FilteredUploadResource(scenario.vehicleRepository, scenario.filterRepository,
                scenario.groupRepository);
        tested.setSettings(settings);
    }

    @Test
    public void uploadFileToFirstMatchFilterDir() throws IOException {
        filterSequence.addFilter(falseVehicleFilter);
        filterSequence.addFilter(trueFilenameFilter);
        filterSequence.addFilter(trueGroupFilterWinSeparator);

        tested.uploadFile(vehicle.uic, content, contentDisposition);

        File rootDir = tested.getFiltersRootDirectory();
        File file = new File(rootDir, "trueFilenameFilter" + File.separator + vehicle.getName() + File.separator + filename);

        assertTrue(file.exists());
    }

    @Test
    public void overflowOfMillis() {
        long day1 = tested.calcAge(1);
        assertEquals((long)1 * 24 * 60 * 60 * 1000, day1);
        long day30 = tested.calcAge(30);
        assertEquals((long)30 * 24 * 60 * 60 * 1000, day30);
    }

    /*
    @Test
    public void uploadFileToDirectoryWithWindowsSeparator() throws IOException {
        filterSequence.addFilter(trueGroupFilterWinSeparator);

        tested.uploadFile(vehicle.uic, content, contentDisposition);

        File file = new File(incomingDir, "trueGroupFilterWinSeparator" + File.separator + group.name + File.separator + vehicle.name + File.separator + filename);

        assertTrue(file.exists());
    }
    */

    @Test
    public void uploadFileToUnfilteredDirectory() throws IOException {
        filterSequence.addFilter(falseVehicleFilter);

        tested.uploadFile(vehicle.uic, content, contentDisposition);

        ZonedDateTime now = ZonedDateTime.now();
        filename = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + File.separator + vehicle.getName() + " " + now.format(DateTimeFormatter.ofPattern("HH.mm"))  + " " + filename;

        File rootDir = tested.getFiltersRootDirectory();
        File unfilteredDir = new File(rootDir, "Unfiltered");
        File file = new File(unfilteredDir, filename);

        assertTrue(file.exists());
    }

    @Test
    public void rootDirectoryFromSettings() throws IOException {
        File rootDir = tested.getFiltersRootDirectory();

        assertEquals(tempDir, rootDir);
    }

    @Test
    public void ignoresRootFolder_ifFilterHasAbsolutePath() throws IOException {
        UploadFilter vehicleFilter = new UploadFilter("trueVehicle", tempDir.append("filterAbs").getAbsolutePath(), "trueVehicle", "Disabled", "30");
        vehicleFilter.addCondition(ConditionType.VEHICLE_NAME, "Mo*in");
        filterSequence.addFilter(vehicleFilter);

        tested.uploadFile(vehicle.uic, content, contentDisposition);

        File file = tempDir.append("filterAbs").append(filename);
        assertTrue(file.exists());
    }
}