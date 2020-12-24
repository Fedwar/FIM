package fleetmanagement.backend.vehiclecommunication.upload;

import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.backend.diagnosis.VersionInfo.VersionType;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.vehiclecommunication.upload.exceptions.UploadFileNotLicenced;
import fleetmanagement.backend.vehicles.DiagnosticSummary;
import fleetmanagement.backend.vehicles.DiagnosticSummary.DiagnosticSummaryType;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class DiagnosisUploadListenerTest {

	private TestScenario scenario;
	private DiagnosisUploadListener tested;
	private Vehicle vehicle;
	private SessionStub session;
	private static NotificationService notificationService = mock(NotificationService.class);

	@Mock
	private DiagnosisHistoryRepository diagnosisHistoryRepository;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		session = new SessionStub();
		scenario = new TestScenario();
		vehicle = scenario.addVehicle();
		SnapshotConversionService snapshotConversionService = new SnapshotConversionService(scenario.diagnosisRepository, diagnosisHistoryRepository, scenario.vehicleRepository);
		tested = new DiagnosisUploadListener(snapshotConversionService, scenario.licence, notificationService);
	}

	@Test
	public void parsesUploadedJson() throws UploadFileNotLicenced {
		String json = "{" +
				"  components: [" +
				"    {" +
				"      id: 0xA0B184," +
				"      name: 'Seat Display 5'," +
				"      type: 'Seat Display'," +
				"      location: 106," +
				"      status: {" +
				"        description: OK," +
				"        code: 0," +
				"        operational: ok" +
				"      }," +
				"      versions: {" +
				"        software: 48.5000.0264V01.8," +
				"        font: 0148.5000.02641140725" +
				"      }" +
				"    }" +
				"  ]" +
				"}";
		uploadJson(json);
		
		Diagnosis diagnosis = scenario.diagnosisRepository.tryFindByVehicleId(vehicle.id);
		DiagnosedDevice component = diagnosis.getDevice("0xA0B184");
		assertEquals("0xA0B184", component.getId());
		assertEquals("Seat Display 5", component.getName().get(session.getAcceptableLanguages()));
		assertEquals("Seat Display", component.getType());
		assertEquals("106", component.getLocation());
		assertEquals("OK", component.getCurrentState().get(0).message.get(session.getAcceptableLanguages()));
		assertEquals("0", component.getCurrentState().get(0).code);
		assertTrue(ChronoUnit.SECONDS.between(ZonedDateTime.now(), component.getCurrentState().get(0).start) <= 1);
		assertNull(component.getCurrentState().get(0).end);
		assertEquals(ErrorCategory.OK, component.getCurrentState().get(0).category);
		assertTrue(component.getCurrentState().get(0).isOk());
		assertEquals("48.5000.0264V01.8", component.getVersion(VersionType.Software));
		assertEquals("0148.5000.02641140725", component.getVersion(VersionType.Fontware));
	}

	@Test
	public void refreshesDiagnosticSummary() throws UploadFileNotLicenced {
		DiagnosticSummary outdated = vehicle.getDiagnosticSummary(ZonedDateTime.now());

		String json = "{" +
				"  components: [" +
				"    {" +
				"      id: 0xA0B184," +
				"      name: 'Seat Display 5'," +
				"      type: 'Seat Display'," +
				"      location: 106," +
				"      status: {" +
				"        description: 'Backlight failed'," +
				"        code: 1," +
				"        operational: broken" +
				"      }," +
				"      versions: {}" +
				"    }" +
				"  ]" +
				"}";
		uploadJson(json);

		DiagnosticSummary updated = vehicle.getDiagnosticSummary(ZonedDateTime.now());
		assertEquals(DiagnosticSummaryType.Ok, outdated.type);
		assertEquals(DiagnosticSummaryType.DeviceErrors, updated.type);
	}

	@Test
	public void savesDiagnosticSummary() throws UploadFileNotLicenced {
		DiagnosticSummary outdated = vehicle.getDiagnosticSummary(ZonedDateTime.now());

		String json = "{" +
				"  components: [" +
				"    {" +
				"      id: 0xA0B184," +
				"      name: 'Seat Display 5'," +
				"      type: 'Seat Display'," +
				"      location: 106," +
				"      status: {" +
				"        description: 'Backlight failed'," +
				"        code: 1," +
				"        operational: broken" +
				"      }," +
				"      versions: {}" +
				"    }" +
				"  ]" +
				"}";
		uploadJson(json);

		DiagnosticSummary updated = vehicle.getDiagnosticSummary(ZonedDateTime.now());
		assertEquals(DiagnosticSummaryType.Ok, outdated.type);
		assertEquals(DiagnosticSummaryType.DeviceErrors, updated.type);
	}

	@Test
	public void usesCurrentTimeAsTimestamp() throws UploadFileNotLicenced {
		uploadJson("{components: []}");
		
		Diagnosis diagnosis = scenario.diagnosisRepository.tryFindByVehicleId(vehicle.id);
		long secondsSinceLastUpdate = diagnosis.getLastUpdated().until(ZonedDateTime.now(), ChronoUnit.SECONDS);
		
		assertTrue(secondsSinceLastUpdate < 5);
	}
	
	@Test
	public void determinesIfFileCanBeHandled() {
		assertFalse(tested.canHandleUploadedFile("unknown-stuff.data"));
		assertTrue(tested.canHandleUploadedFile("diagnosis.json"));
	}
	
	private void uploadJson(String json) throws UploadFileNotLicenced {

		String filename = "diagnosis.json";
		tested.onFileUploaded(vehicle.id, filename, json.getBytes(Charset.forName("UTF-8")));
	}
}