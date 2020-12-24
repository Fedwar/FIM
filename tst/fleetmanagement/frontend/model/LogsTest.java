package fleetmanagement.frontend.model;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import org.apache.commons.io.IOUtils;
import org.junit.*;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;

public class LogsTest {
	
	private Logs tested;
	private Vehicle vehicle;
	private fleetmanagement.backend.tasks.Task task;

	@Before
	public void setUp() throws IOException {
		TestScenario scenario = new TestScenario();
		Package pkg = scenario.addPackage(PackageType.DataSupply, "1.0",
				1, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		vehicle = scenario.addVehicle();
		task = scenario.addTask(vehicle, pkg);
		task.addLog(new LogEntry(Severity.INFO, "test"));
		
		tested = new Logs(scenario.vehicleRepository, scenario.taskRepository);
	}
	
	@Test
	public void collectsLogsAndProvidesZipStream() throws IOException {
		InputStream stream = tested.getAsZipStream();
		
		Map<String, byte[]> filesInZip = unpackZip(stream);
		assertNonEmptyFileInZip("fleetmanagement.log", filesInZip);
		assertNonEmptyFileInZip("vehicle-" + vehicle.id + "/task-" + task.getId() + "-log.txt" , filesInZip);
	}

	private Map<String, byte[]> unpackZip(InputStream stream) throws IOException {
		ZipInputStream zipStream = new ZipInputStream(stream);
		
		Map<String, byte[]> filesInZip = new HashMap<>();
		ZipEntry entry;
		while ((entry = zipStream.getNextEntry()) != null)
			filesInZip.put(entry.getName(), IOUtils.toByteArray(zipStream));
		return filesInZip;
	}

	private void assertNonEmptyFileInZip(String name, Map<String, byte[]> filesInZip) {
		assertTrue(filesInZip.get(name).length > 0);
	}

}
