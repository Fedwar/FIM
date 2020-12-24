package fleetmanagement.backend;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.*;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageInstallationStatus.State;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus.*;
import fleetmanagement.test.TestScenarioPrefilled;


public class PackageInstallationStatusTest {
	
	private PackageInstallationStatus tested;
	private TestScenarioPrefilled scenario;
	
	@Before
	public void setup() {
		scenario = new TestScenarioPrefilled();
	}
	
	@Test
	public void determinesNoInstallationIsRunning() {
		determineInstallationStatus();
		
		assertEquals(State.NotInstalled, tested.state);
	}
	
	@Test
	public void determinesIfInstallationIsPending() throws IOException {
		scenario.addTask(scenario.vehicle1, scenario.package1);
		
		determineInstallationStatus();
		
		assertEquals(State.InstallationUpcoming, tested.state);
		assertEquals(0, tested.progressPercent);
	}
	
	@Test
	public void determinesIfInstallationIsRunning() throws IOException {
		Task t = scenario.addTask(scenario.vehicle1, scenario.package1);
		t.setClientStatus(ClientStage.DOWNLOADING, 17);
		t.setServerStatus(ServerStatus.Running);
		
		determineInstallationStatus();
		
		assertEquals(State.InstallationUpcoming, tested.state);
		assertEquals(17, tested.progressPercent);
	}
	
	@Test
	public void determinesIfRepairInstallationIsPending() throws IOException {
		scenario.vehicle1.versions.setDataSupplyVersion(1, "3.5");
		Package pkg = scenario.addPackage(PackageType.DataSupply, "3.5", 1,  "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		scenario.addTask(scenario.vehicle1, pkg);
		
		determineInstallationStatus(pkg);
		
		assertEquals(State.InstallationUpcoming, tested.state);
		assertEquals(0, tested.progressPercent);
	}
	
	@Test
	public void detectsThatPackageIsAlreadyInstalled() throws IOException {
		scenario.vehicle1.versions.setDataSupplyVersion(1, "3.5");
		Package pkg = scenario.addPackage(PackageType.DataSupply, "3.5", 1,  "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		
		determineInstallationStatus(pkg);
		
		assertEquals(State.Installed, tested.state);
		assertEquals(100, tested.progressPercent);
	}
	
	@Test
	public void packageIsNotInstalledIfSlotDiffers() throws IOException {
		scenario.vehicle1.versions.setDataSupplyVersion(2, "3.5");
		Package pkg = scenario.addPackage(PackageType.DataSupply, "3.5", 1,  "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		
		determineInstallationStatus(pkg);
		
		assertEquals(State.NotInstalled, tested.state);
	}
	
	@Test
	public void determinesIfInstallationWasCancelled() throws IOException {
		Task t = scenario.addTask(scenario.vehicle1, scenario.package1);
		t.cancel();
		
		determineInstallationStatus();
		
		assertEquals(State.NotInstalled, tested.state);
	}

	private void determineInstallationStatus() {
		determineInstallationStatus(scenario.package1);
	}
	
	private void determineInstallationStatus(Package pkg) {
		tested = PackageInstallationStatus.create(pkg, scenario.vehicle1, scenario.taskRepository);
	}
}
