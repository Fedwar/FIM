package fleetmanagement.frontend.controllers;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.*;

import javax.ws.rs.core.Response;

import fleetmanagement.backend.vehiclecommunication.FilteredUploadResource;
import fleetmanagement.backend.vehiclecommunication.upload.filter.*;
import fleetmanagement.config.Settings;
import org.junit.Before;
import org.junit.Test;

import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.test.*;
import gsp.zip.*;
import org.mockito.Mock;

public class AdminTest {
	
	private Admin tested;
	private TestScenarioPrefilled scenario;

	@Before
	public void setup() {
		scenario = new TestScenarioPrefilled();
		tested = new Admin(new SessionStub(), scenario.vehicleRepository, scenario.taskRepository,
				scenario.packageTypeRepository, scenario.groupRepository, scenario.licence, null);
	}
	
	@Test
	public void showsAdminUi() {
		ModelAndView<fleetmanagement.frontend.model.Admin> vm = tested.getAdminUI();
		assertEquals(vm.page, "admin-logs.html");
	}

	/*
	@Test
	public void generatesZipWithLogFiles() throws IOException {
		fleetmanagement.backend.tasks.Task task = scenario.addTask(scenario.vehicle1, scenario.package1);

		Response r = tested.downloadAllLogs();

		InMemoryFileSystem fs = unzip((InputStream)r.getEntity());
		String tasklog = "vehicle-" + scenario.vehicle1.id + File.separator + "task-" + task.id() + "-log.txt";
		assertTrue(fs.exists(new File("fleetmanagement.log")));
		assertTrue(fs.exists(new File(tasklog)));
		assertEquals("application/x-zip-compressed", r.getMetadata().getFirst("Content-Type").toString());
	}
	*/


	private InMemoryFileSystem unzip(InputStream entity) throws IOException {
		InMemoryFileSystem fs = new InMemoryFileSystem();
		Zip zip = new Zip(fs);
		zip.unpack(entity, new File("."));
		return fs;
	}
}
