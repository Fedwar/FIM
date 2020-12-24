package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.repositories.exception.GroupDuplicationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class OnDiskGroupRepositoryTest {
    private OnDiskGroupRepository tested;
    private Group group1;
    private Group group2;

    @Rule
    public TempFileRule tempDir = new TempFileRule();

    @Before
    public void setUp() {
        tested = new OnDiskGroupRepository(tempDir);
        group1 = addGroup("group1", "group1", false);
        group2 = addGroup("group2", "group2", true);
    }

    @Test
    public void loadFromDisk() throws IOException {
        tested = new OnDiskGroupRepository(tempDir);
        tested.loadFromDisk();

        assertEquals(2, tested.listAll().size());

        Group loaded1 = tested.tryFindById(group1.id);
        Group loaded2 = tested.tryFindById(group2.id);

        assertEquals(group1.id, loaded1.id);
        assertEquals(group1.name, loaded1.name);
        assertEquals(group1.dir, loaded1.dir);
        assertEquals(group1.isAutoSyncEnabled, loaded1.isAutoSyncEnabled);

        assertEquals(group2.id, loaded2.id);
        assertEquals(group2.name, loaded2.name);
        assertEquals(group2.dir, loaded2.dir);
        assertEquals(group2.isAutoSyncEnabled, loaded2.isAutoSyncEnabled);
    }

    @Test
    public void insertGroupWithNullId() {
        Group group = new Group(null, "groupNull", "groupNull", false);
        tested.insert(group);

        assertEquals(3, tested.listAll().size());
    }

    @Test
    public void insert() {
        Group group = addGroup("group3", "group3", false);

        Group inserted = tested.tryFindById(group.id);
        assertEquals(group.id, inserted.id);
        assertEquals(group.name, inserted.name);
        assertEquals(group.dir, inserted.dir);
        assertEquals(group.isAutoSyncEnabled, inserted.isAutoSyncEnabled);
    }

    @Test
    public void persistsInProperFolder() {
        Group group = addGroup("group3", "group3", false);
        tested = new OnDiskGroupRepository(tempDir);
        tested.loadFromDisk();

        File groupFolder3 = new File(tempDir.getAbsolutePath() + File.separator + group.id);
        File groupXMLFile3 = new File(groupFolder3.getAbsolutePath() + File.separator + "group.xml");
        assertTrue(groupFolder3.isDirectory());
        assertTrue(groupXMLFile3.isFile());
    }

    @Test(expected = GroupDuplicationException.class)
    public void insertThrowsExceptionWhenGroupExistsInList() {
        tested.insert(group1);
    }

    @Test
    public void update() {
        tested.update(group1.id, group3 -> {
            group3.name = "group3";
            group3.dir = "group3";
        });
        tested = new OnDiskGroupRepository(tempDir);
        tested.loadFromDisk();

        Group group = tested.tryFindById(group1.id);
        assertEquals("group3", group.name);
        assertEquals("group3",
                group.dir);
    }

    @Test
    public void delete() {
        tested.delete(group1.id);

        assertEquals(1, tested.listAll().size());
        assertNull(tested.tryFindById(group1.id));
    }

    @Test
    public void listAll() {
        assertEquals(2, tested.listAll().size());
    }

    @Test
    public void tryFindById() {
        assertEquals(group1, tested.tryFindById(group1.id));
        assertEquals(group2, tested.tryFindById(group2.id));
    }

    @Test
    public void mapAll() {
        Map<String, Group> map = tested.mapAll();

        assertTrue(map.containsKey(group1.id.toString()));
        assertTrue(map.containsKey(group2.id.toString()));
        assertTrue(map.containsValue(group1));
        assertTrue(map.containsValue(group2));
    }

    private Group addGroup(String name, String dir, boolean isAutoSyncEnabled) {
        Group group = new Group(name, dir, isAutoSyncEnabled);
        tested.insert(group);
        return group;
    }

    @Test
    public void noException_WhenXmlFileIsEmpty() throws Exception {
        TempFile folder = tempDir.newFolder(UUID.randomUUID().toString());

        String xmlFile = tested.getXmlFile(folder).file().getName();
        folder.newFile(xmlFile);

        tested.loadFromDisk();
    }

}