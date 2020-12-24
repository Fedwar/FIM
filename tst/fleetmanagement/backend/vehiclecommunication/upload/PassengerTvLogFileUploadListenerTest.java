package fleetmanagement.backend.vehiclecommunication.upload;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.*;

import fleetmanagement.backend.vehiclecommunication.FilteredUploadResource;
import fleetmanagement.test.TestScenario;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import gsp.testutil.TemporaryDirectory;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class PassengerTvLogFileUploadListenerTest {
	private PassengerTvLogFileUploadListener tested;
	TestScenarioPrefilled scenario;
	@Mock
	public FilteredUploadResource filteredUploadResource;
	
	@Before
	public void setup() {
		filteredUploadResource = mock(FilteredUploadResource.class);
		//MockitoAnnotations.initMocks(this);
		scenario = new TestScenarioPrefilled();
		tested = new PassengerTvLogFileUploadListener(scenario.vehicleRepository, filteredUploadResource);
	}
	
	@Test
	public void onlyHandlesPassengerTvLogFiles() {
		assertTrue(tested.canHandleUploadedFile("2018-06-24_123456789013_ET474.csv"));
		assertTrue(tested.canHandleUploadedFile("2018-06-24_123456789013_et474.CSV"));
		assertFalse(tested.canHandleUploadedFile("Something_else.csv"));
	}

	@Test
	public void delegatesToFilteredUploadRecource() throws IOException {
			tested.onFileUploaded(scenario.vehicle1.id, "2018-06-24_123456789013_ET474.csv", "Log-Data".getBytes("UTF-8"));
			verify(filteredUploadResource).onFileReceived(scenario.vehicle1, "2018-06-24_123456789013_ET474.csv", "Log-Data".getBytes("UTF-8"));
	}

}
