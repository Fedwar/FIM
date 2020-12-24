package fleetmanagement.backend.vehiclecommunication.upload;

import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.vehiclecommunication.upload.exceptions.UploadFileNotLicenced;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.Charsets;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OperationDataUploadListenerTest {

    private TestScenarioPrefilled scenario;
    private OperationDataUploadListener tested;
    private OperationDataRepository operationDataRepository;
    private String json;
    private String json1;
    private NotificationService notificationService;
    private UUID vehicleId;

    @Before
    public void setup() {
        scenario = new TestScenarioPrefilled();
        operationDataRepository = scenario.operationDataRepository;
        notificationService = mock(NotificationService.class);
        tested = new OperationDataUploadListener(operationDataRepository, scenario.licence, notificationService);

        json = "{'dieseltank1': {     'value': '2330',     'unit': 'liter'   }" +
                ",'dieseltank2': {     'value': 65,     'unit': 'liter'   }" +
                ",'lowFuel': {     'value': true   }}";

        json1 = "{'value': '2330', 'unit': 'liter' }";

        vehicleId = scenario.vehicle1.id;
    }

    @Test
    public void operationDataValuesUpdated() throws InterruptedException, UploadFileNotLicenced {
        OperationData operationData = scenario.addOperationData(vehicleId,
                new Indicator("dieseltank1", "12", "liter", ZonedDateTime.now()),
                new Indicator("dieseltank2", "34", "liter", ZonedDateTime.now()) );
        ZonedDateTime updated = operationData.updated;
        Indicator dieseltank1 = operationData.getIndicator("dieseltank1");
        ZonedDateTime indicatorUpdated = operationData.updated;

        Thread.sleep(100);

        tested.onFileUploaded(vehicleId, "", json.getBytes(Charsets.UTF_8));

        operationData = operationDataRepository.tryFindById(vehicleId);

        assertNotEquals("indicator value insertField not changed", updated, operationData.updated);
        assertTrue("indicator value insertField not changed", dieseltank1.value.equals("2330"));
        assertNotEquals("indicator updated insertField not changed", indicatorUpdated, dieseltank1.updated);
    }

    @Test
    public void onFileUploaded() throws UploadFileNotLicenced {
        tested.onFileUploaded(vehicleId, "", json.getBytes(Charsets.UTF_8));

        OperationData operationData = operationDataRepository.tryFindById(vehicleId);
        Indicator dieseltank1 = operationData.getIndicator("dieseltank1");
        assertNotNull(operationData);
        assertNotNull(dieseltank1);
        assertNotNull(dieseltank1.updated);
        assertEquals("2330", dieseltank1.value);
        assertEquals("liter", dieseltank1.unit);
    }

    @Test()
    public void onFileUploaded_InvalidFormat() throws UploadFileNotLicenced {
        tested.onFileUploaded(vehicleId, "", json1.getBytes(Charsets.UTF_8));
        OperationData operationData = operationDataRepository.tryFindById(vehicleId);
        assertNull(operationData);
    }

    @Test
    public void onFileUploaded_EmptyFile() throws UploadFileNotLicenced {
        tested.onFileUploaded(vehicleId, "", "".getBytes(Charsets.UTF_8));
        OperationData operationData = operationDataRepository.tryFindById(vehicleId);
        assertNull(operationData);
    }

    @Test
    public void onFileUploaded_EmptyJson() throws UploadFileNotLicenced {
        tested.onFileUploaded(vehicleId, "", "{}".getBytes(Charsets.UTF_8));
        OperationData operationData = operationDataRepository.tryFindById(vehicleId);
        assertNotNull(operationData);
    }

    @Test(expected = UploadFileNotLicenced.class)
    public void exceptionWhenNotLicenced() throws UploadFileNotLicenced {
        scenario.licence.operationInfo = false;
        tested.onFileUploaded(vehicleId, "", json.getBytes(Charsets.UTF_8));
    }

    @Test
    public void onFileUploaded_NotificationEventTriggered() throws UploadFileNotLicenced {
        tested.onFileUploaded(vehicleId, "", "{}".getBytes(Charsets.UTF_8));
        verify(notificationService).processEvent(any(Event.class));
    }

    private OperationData newOperationData(Indicator... indicators) {
        OperationData operationData = new OperationData(vehicleId, ZonedDateTime.now(),  Arrays.asList(indicators));
        return operationData;
    }


}