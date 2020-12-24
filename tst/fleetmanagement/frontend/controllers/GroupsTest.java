package fleetmanagement.frontend.controllers;

import com.google.gson.Gson;
import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupInstaller;
import fleetmanagement.backend.groups.GroupsWatcher;
import fleetmanagement.backend.packages.PackageImportService;
import fleetmanagement.config.Settings;
import fleetmanagement.frontend.TempDirectory;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import fleetmanagement.usecases.ImportPackage;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GroupsTest {
    private static final Logger logger = Logger.getLogger(GroupsTest.class);
    private static final Gson gson = new Gson();

    @Mock
    public Settings settings;
    private TestScenarioPrefilled scenario;
    private Groups tested;
    @Rule
    public TempFileRule tempFolder = new TempFileRule();
    public TempFile groupsFolder;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private GroupsWatcher watcher;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        watcher = mock(GroupsWatcher.class);
        scenario = new TestScenarioPrefilled();
        scenario.groupRepository.listAll().forEach(group -> scenario.groupRepository.delete(group.id));

        groupsFolder = tempFolder.newFolder("groupDir");
        when(settings.getImportFolderPath()).thenReturn(groupsFolder.getCanonicalPath());

        PackageImportService packageImportService = new PackageImportService(scenario.packageRepository
                , new ImportPackage(scenario.packageRepository, new TempDirectory(tempFolder.newFolder("Temp")), scenario.licence, scenario.preprocessor)
                , scenario.notificationService);
        GroupInstaller groupInstaller = new GroupInstaller(scenario.vehicleRepository, scenario.packageRepository
                , scenario.taskRepository, scenario.packageTypeRepository, scenario.installPackage, scenario.deletePackage);
        watcher = new GroupsWatcher(packageImportService, groupInstaller, scenario.groupRepository);
        watcher.setSettings(settings);
        watcher.initRootDirectory();

        tested = new Groups(new SessionStub(), scenario.groupRepository, scenario.vehicleRepository
                , watcher, scenario.packageRepository, groupInstaller);

    }

    @Test
    public void addGroupAddsNewGroupToRepositoryWhenFolderDoesNotExist() {
        String json = "{\"name\":\"group1\",\"dir\":\"group1\"}";

        tested.addGroup(json);

        assertEquals(1, scenario.groupRepository.listAll().size());
    }

    @Test
    public void addGroupCreatesNewFolderWhenFolderDoesNotExist() {
        String json = "{\"name\":\"group1\",\"dir\":\"group1\"}";
        tested.addGroup(json);

        File group1 = new File(groupsFolder, "group1");
        assertTrue(group1.isDirectory());
    }

    @Test
    public void addGroupAddsNewGroupToRepositoryWhenFolderExists() {
        groupsFolder.newFolder("group1");
        String json = "{\"name\":\"group1\",\"dir\":\"group1\"}";
        tested.addGroup(json);

        assertEquals(1, scenario.groupRepository.listAll().size());
    }

    @Test
    public void addGroupAddsNewGroupToRepositoryWhenEnteredFolderNameWithoutPath() throws IOException {
        String json = "{\"name\":\"group1\",\"dir\":\"group1\"}";

        tested.addGroup(json);

        assertEquals(1, scenario.groupRepository.listAll().size());

        File group1 = new File(new File("group1").getCanonicalPath());
        if (!group1.delete())
            logger.error("Can not delete directory " + group1.getAbsolutePath());
    }

    @Test
    public void addGroupCreatesNewFolderWhenEnteredFolderNameWithoutPath() {
        String json = "{\"name\":\"group1\",\"dir\":\"group1\"}";

        tested.addGroup(json);

        File group1 = new File(groupsFolder, "group1");
        assertTrue(group1.isDirectory());

        if (!group1.delete())
            logger.error("Can not delete directory " + group1.getAbsolutePath());
    }

    @Test
    public void addGroupDoesNotAddNewGroupToRepositoryWhenPathDoesNotExist() {
        String path = (temporaryFolder.getRoot().getAbsolutePath() + File.separator + "abra-kadabra" +
                File.separator + "group1").replace(File.separator, File.separator + File.separator);
        String json = "{\"name\":\"group1\",\"dir\":\"" + path + "\"}";

        tested.addGroup(json);

        assertEquals(0, scenario.groupRepository.listAll().size());
    }

    @Test
    public void editGroupChangesGroupNameAndDirWhenFolderDoesNotExist() {
        Group oldGroup = new Group("group1", "group1", false);
        tested.addGroup(gson.toJson(oldGroup));
        String id = oldGroup.id.toString();
        String newJson = "{\"name\":\"group2\",\"dir\":\"group2\"}";

        tested.editGroup(id, newJson);

        Group newGroup = scenario.groupRepository.tryFindById(UUID.fromString(id));
        assertEquals("group2", newGroup.name);
        assertEquals("group2", newGroup.dir);
    }

    @Test
    public void editGroupCreatesNewFolderWhenFolderDoesNotExist() {
        Group oldGroup = new Group("group1", "group1", false);
        tested.addGroup(gson.toJson(oldGroup));
        String id = oldGroup.id.toString();
        String newJson = "{\"name\":\"group2\",\"dir\":\"group2\"}";

        tested.editGroup(id, newJson);

        assertTrue(new File(groupsFolder, "group2").isDirectory());
    }

    @Test
    public void editGroupChangesGroupNameAndDir() {
        Group oldGroup = new Group("group1", "group1", false);
        tested.addGroup(gson.toJson(oldGroup));
        String id = oldGroup.id.toString();
        String newJson = "{\"name\":\"group2\",\"dir\":\"group2\"}";

        tested.editGroup(id, newJson);

        Group newGroup = scenario.groupRepository.tryFindById(UUID.fromString(id));
        assertEquals("group2", newGroup.name);
        assertEquals("group2", newGroup.dir);
    }

    @Test
    public void editGroup_ChangesGroupNameAndDir_WhenEnteredFolderNameWithoutPath() {
        Group oldGroup = new Group("group1", "group1", false);
        tested.addGroup(gson.toJson(oldGroup));
        String id = oldGroup.id.toString();
        String newJson = "{\"name\":\"group2\",\"dir\":\"group2\"}";

        tested.editGroup(id, newJson);

        Group newGroup = scenario.groupRepository.tryFindById(UUID.fromString(id));
        assertEquals("group2", newGroup.name);
        assertEquals("group2", newGroup.dir);

        if (!(new File(oldGroup.dir).delete()))
            logger.error("Can not delete directory " + oldGroup.dir);
        if (!(new File(newGroup.dir).delete()))
            logger.error("Can not delete directory " + newGroup.dir);
    }

    @Test
    public void editGroup_CreatesNewFolder_WhenEnteredFolderNameWithoutPath() {
        Group oldGroup = new Group("group1", "group1", false);
        tested.addGroup(gson.toJson(oldGroup));
        String id = oldGroup.id.toString();
        String newJson = "{\"name\":\"group2\",\"dir\":\"group2\"}";

        tested.editGroup(id, newJson);

        Group newGroup = scenario.groupRepository.tryFindById(UUID.fromString(id));

        assertTrue(new File(groupsFolder, oldGroup.dir).isDirectory());
        assertTrue(new File(groupsFolder, newGroup.dir).isDirectory());
    }

    @Test
    public void editGroup() {
        Group group = new Group("group1",
                "group1", false);
        tested.addGroup(gson.toJson(group));

        group.name = "group2";
        group.dir = "group2";
        group.isAutoSyncEnabled = true;

        tested.editGroup(group.id.toString(), gson.toJson(group));

        Group newGroup = scenario.groupRepository.tryFindById(group.id);

        assertEquals(group.name, newGroup.name);
        assertEquals(group.dir, newGroup.dir);
        assertEquals(group.isAutoSyncEnabled, newGroup.isAutoSyncEnabled);
    }

    @Test
    public void assignPackage_AssignsPackageToSeveralGroups() {
        Group group1 = new Group("group1", temporaryFolder.getRoot().getAbsolutePath(), false);
        Group group2 = new Group("group2", temporaryFolder.getRoot().getAbsolutePath(), false);

        scenario.groupRepository.insert(group1);
        scenario.groupRepository.insert(group2);

        String groupId1 = group1.id.toString();
        String groupId2 = group2.id.toString();

        scenario.vehicle1.setGroupId(groupId1);
        scenario.vehicle2.setGroupId(groupId1);
        scenario.vehicle3.setGroupId(groupId2);

        String groupsJson = "[\"" + groupId1 + "\",\"" + groupId2 + "\"]";

        tested.assignPackage(scenario.package1.id.toString(), groupsJson);

        assertEquals(1, scenario.vehicle1.getRunningTasks(scenario.taskRepository).size());
        assertEquals(1, scenario.vehicle2.getRunningTasks(scenario.taskRepository).size());
        assertEquals(1, scenario.vehicle3.getRunningTasks(scenario.taskRepository).size());
    }

    @Test
    public void assignVehicles_AssignsVehicles_WhenGroupDoesNotEqualNull() {
        Group group = new Group("group1",
                temporaryFolder.getRoot().getAbsolutePath(), false);
        scenario.groupRepository.insert(group);
        String groupId = group.id.toString();
        String vehiclesJson = "[\"" + scenario.vehicle1.id.toString() + "\",\"" +
                scenario.vehicle2.id.toString() + "\"]";

        tested.assignVehicles(groupId, vehiclesJson);

        assertEquals(groupId, scenario.vehicle1.getGroupId());
        assertEquals(groupId, scenario.vehicle2.getGroupId());
    }

    @Test
    public void assignVehicles_DoesNotAssignsVehicles_WhenGroupEqualsNull() {
        String groupId = "cdbb69c2-5d9b-4244-99d4-f56c7c3a2ccd";
        String vehiclesJson = "[\"" + scenario.vehicle1.id.toString() + "\",\"" +
                scenario.vehicle2.id.toString() + "\"]";

        tested.assignVehicles(groupId, vehiclesJson);

        assertNull(scenario.vehicle1.getGroupId());
        assertNull(scenario.vehicle2.getGroupId());
    }

    @Test
    public void assignPackages_AssignsPackages_WhenGroupDoesNotEqualNull() {
        Group group = new Group("group1", temporaryFolder.getRoot().getAbsolutePath(), false);
        scenario.groupRepository.insert(group);
        String groupId = group.id.toString();
        scenario.vehicle1.setGroupId(groupId);
        scenario.vehicle2.setGroupId(groupId);
        String packagesJson = "[\"" + scenario.package1.id.toString() + "\",\"" +
                scenario.package2.id.toString() + "\"]";

        tested.assignPackages(groupId, packagesJson);

        assertEquals(2, scenario.vehicle1.getRunningTasks(scenario.taskRepository).size());
        assertEquals(2, scenario.vehicle2.getRunningTasks(scenario.taskRepository).size());
    }

    @Test
    public void assignPackages_DoesNotAssignsPackages_WhenGroupEqualsNull() {
        String groupId = "cdbb69c2-5d9b-4244-99d4-f56c7c3a2ccd";
        scenario.vehicle1.setGroupId(groupId);
        scenario.vehicle2.setGroupId(groupId);
        String packagesJson = "[\"" + scenario.package1.id.toString() + "\",\"" +
                scenario.package2.id.toString() + "\"]";

        tested.assignPackages(groupId, packagesJson);

        assertEquals(0, scenario.package1.numberOfRunningTasks(scenario.taskRepository));
        assertEquals(0, scenario.package2.numberOfRunningTasks(scenario.taskRepository));
        assertEquals(0, scenario.vehicle1.getRunningTasks(scenario.taskRepository).size());
        assertEquals(0, scenario.vehicle2.getRunningTasks(scenario.taskRepository).size());
    }

    @Test
    public void removeVehicles() {
        Group group = new Group("group1",
                temporaryFolder.getRoot().getAbsolutePath() + File.separator + "group1", false);
        tested.addGroup(gson.toJson(group));
        String groupId = group.id.toString();
        String vehiclesJson = "[\"" + scenario.vehicle1.id.toString() + "\",\"" +
                scenario.vehicle2.id.toString() + "\"]";
        tested.assignVehicles(groupId, vehiclesJson);

        tested.removeVehiclesFromGroup(vehiclesJson);

        assertEquals(0, scenario.vehicleRepository.listByGroup(groupId).size());
        assertNull(scenario.vehicle1.getGroupId());
        assertNull(scenario.vehicle2.getGroupId());
    }

    @Test
    public void deleteGroup_DeletesGroupFromRepository() {
        Group group = new Group("group1",
                temporaryFolder.getRoot().getAbsolutePath() + File.separator + "group1", false);
        tested.addGroup(gson.toJson(group));
        String groupId = group.id.toString();

        tested.deleteGroup(groupId);

        assertEquals(0, scenario.groupRepository.listAll().size());
    }

    @Test
    public void deleteGroup_RemovesIdsOfDeletedGroupFromVehicles() {
        Group group = new Group("group1",
                temporaryFolder.getRoot().getAbsolutePath() + File.separator + "group1", false);
        tested.addGroup(gson.toJson(group));
        String groupId = group.id.toString();
        String vehiclesJson = "[\"" + scenario.vehicle1.id.toString() + "\",\"" +
                scenario.vehicle2.id.toString() + "\"]";
        tested.assignVehicles(groupId, vehiclesJson);

        tested.deleteGroup(groupId);

        assertNull(scenario.vehicle1.getGroupId());
        assertNull(scenario.vehicle2.getGroupId());
    }

    @Test
    public void deleteGroup_RemovesAllPackagesByGroupId() {
        Group group = new Group("group1",
                temporaryFolder.getRoot().getAbsolutePath() + File.separator + "group1", false);
        scenario.groupRepository.insert(group);

        String groupId = group.id.toString();
        String groupsJson = "[\"" + groupId + "\"]";

        tested.assignPackage(scenario.package1.id.toString(), groupsJson);
        tested.assignPackage(scenario.package2.id.toString(), groupsJson);

        tested.deleteGroup(groupId);

        assertEquals(1, scenario.packageRepository.getDuplicates(scenario.package1).size());
        assertEquals(1, scenario.packageRepository.getDuplicates(scenario.package2).size());
    }
}