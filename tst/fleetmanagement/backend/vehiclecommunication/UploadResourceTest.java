package fleetmanagement.backend.vehiclecommunication;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;

import fleetmanagement.backend.vehiclecommunication.upload.exceptions.UploadFileNotLicenced;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.webserver.UnknownVehicleRequest;
import org.junit.*;
import org.mockito.*;

import com.sun.jersey.core.header.FormDataContentDisposition;

import fleetmanagement.test.TestScenarioPrefilled;
import gsp.testutil.TemporaryDirectory;

public class UploadResourceTest {

	private UploadResource tested;
	@Mock private FileUploadListener listener;
	private byte[] contentBytes;
	private ByteArrayInputStream content;
	private FormDataContentDisposition contentDisposition;
	private TemporaryDirectory tempDir; 
	private TestScenarioPrefilled scenario = new TestScenarioPrefilled();
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		
		contentBytes = new byte[]{1,2,3,4,5};
		content = new ByteArrayInputStream(contentBytes);
		contentDisposition = FormDataContentDisposition.name("file").fileName("myFile.bin").size(5).build();
		scenario.licence.vehicleIp = false;

		tempDir = TemporaryDirectory.create();
		
		tested = new UploadResource(scenario.vehicleRepository, tempDir, scenario.licence);
		tested.addListener(listener);
	}
	
	@After
	public void tearDown() {
		tempDir.delete();
	}

	@Test
	public void asksListenersIfFileCanBeHandled() throws IOException {
		tested.uploadFile(scenario.vehicle1.uic, content, contentDisposition, null);
		
		verify(listener).canHandleUploadedFile("myFile.bin");
	}
	
	@Test
	public void notifiesListenersIfFileCanBeHandled() throws IOException, UploadFileNotLicenced {
		when(listener.canHandleUploadedFile("myFile.bin")).thenReturn(true);
		
		tested.uploadFile(scenario.vehicle1.uic, content, contentDisposition, null);
		
		verify(listener).onFileUploaded(scenario.vehicle1.id, "myFile.bin", contentBytes);
	}

	@Test
	public void doesNotNotifyListenersWhenFileCannotBeHandled() throws IOException, UploadFileNotLicenced {
		tested.uploadFile(scenario.vehicle1.uic, content, contentDisposition, null);
		
		verify(listener, never()).onFileUploaded(any(), any(), any());
	}
	
	@Test
	public void storesUnhandledFile() throws IOException {
		tested.uploadFile(scenario.vehicle1.uic, content, contentDisposition, null);
		
		File unknownFile = new File(tempDir, "myFile.bin");
		assertTrue(unknownFile.exists());
	}

	@Test
	public void doesNotStoreHandledFile() throws IOException {
		when(listener.canHandleUploadedFile("myFile.bin")).thenReturn(true);

		tested.uploadFile(scenario.vehicle1.uic, content, contentDisposition, null);

		File unknownFile = new File(tempDir, "myFile.bin");
		assertFalse(unknownFile.exists());
	}

	@Test
	public void updatesVehicleIp() throws IOException {
		scenario.licence.vehicleIp = true;
		tested.uploadFile(scenario.vehicle1.uic, content, contentDisposition, "vehicleIp");
		Vehicle vehicle = scenario.getVehicle(scenario.vehicle1.id);

		assertEquals("vehicleIp", vehicle.ipAddress);
	}

	@Test
	public void doesNotUpdateVehicleIp_ifNotLicenced() throws IOException {
		scenario.licence.vehicleIp = false;
		tested.uploadFile(scenario.vehicle1.uic, content, contentDisposition, "vehicleIp");
		Vehicle vehicle = scenario.getVehicle(scenario.vehicle1.id);

		assertNull(vehicle.ipAddress);
	}

	@Test
	public void throwsUnknownVehicleRequestWhenVehicleDoesNotExist() throws IOException {
		try {
			tested.uploadFile("NotExistingUic", content, contentDisposition, "vehicleIp");
			fail();
		} catch (UnknownVehicleRequest ignored) {}
	}


}
