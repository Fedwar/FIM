package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.notifications.settings.Type;
import fleetmanagement.test.TestScenario;
import gsp.testutil.TemporaryDirectory;
import org.junit.*;

import javax.mail.internet.AddressException;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OnDiskNotificationSettingRepositoryTest {

    static TestScenario scenario;

    @Rule
    public TempFileRule tempDir = new TempFileRule();

    private OnDiskNotificationSettingRepository tested;

    @BeforeClass
    public static void beforeClass() {
        scenario = new TestScenario();
    }

    @Before
    public void beforeTest() throws Exception {
        tempDir.clean();
        tested = new OnDiskNotificationSettingRepository(tempDir);
    }

    @Test
    public void insert() throws AddressException {

        String mailList = "mail1@mail.com, mail2@mail.com";

        HashMap<Parameter, String> conditions = new HashMap<>();
        conditions.put(Parameter.UPPER_LIMIT, "1");
        conditions.put(Parameter.LOWER_LIMIT, "0");

        NotificationSetting notificationSetting = new NotificationSetting(Type.DIAGNOSED_DEVICE_ERROR, mailList, conditions);
        tested.insert(notificationSetting);

        tested = new OnDiskNotificationSettingRepository(tempDir);
        tested.loadFromDisk();

        NotificationSetting loaded = tested.tryFindById(notificationSetting.id);

        assertNotNull(loaded);
        assertEquals(notificationSetting.type, loaded.type);
        assertEquals(notificationSetting.getMailList(), loaded.getMailList());
        assertEquals(notificationSetting.getParameters(), loaded.getParameters());
    }

    @Test
    public void noException_WhenXmlFileIsEmpty() throws Exception {
        TempFile folder = tempDir.newFolder(UUID.randomUUID().toString());
        String xmlFile = tested.getXmlFile(folder).file().getName();
        folder.newFile(xmlFile);

        tested.loadFromDisk();
    }

}