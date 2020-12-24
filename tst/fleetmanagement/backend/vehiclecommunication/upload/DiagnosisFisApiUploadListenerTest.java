package fleetmanagement.backend.vehiclecommunication.upload;

import fleetmanagement.backend.diagnosis.SnapshotConversionService;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenario;
import gsp.configuration.LocalFiles;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DiagnosisFisApiUploadListenerTest {

    private TestScenario scenario;
    private DiagnosisFisApiUploadListener tested;
    private Vehicle vehicle;
    private SnapshotConversionService snapshotConversionService;
    private SessionStub session;
    private static NotificationService notificationService = mock(NotificationService.class);

    @Before
    public void setup() {
        session = new SessionStub();
        scenario = new TestScenario();
        vehicle = scenario.addVehicle();
        snapshotConversionService = mock(SnapshotConversionService.class);
        tested = new DiagnosisFisApiUploadListener(snapshotConversionService, notificationService);
    }

    @Test
    public void canHandleUploadedFile() {
        assertTrue(tested.canHandleUploadedFile("diagnosis-fis-api.json"));
    }

    @Test
    public void onFileUploaded() throws IOException {

        File jsonFile = LocalFiles.find("test-resources/diagnonsis/diagnosis-fis-api.json");
        byte[] fileContent = Files.readAllBytes(jsonFile.toPath());

        tested.onFileUploaded(vehicle.id, "diagnosis-fis-api.json", fileContent);

        verify(snapshotConversionService).integrateNewSnapshot(any());

    }





}