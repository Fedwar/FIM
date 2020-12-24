package fleetmanagement.backend;

import static org.junit.Assert.*;

import org.junit.*;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.test.TestScenarioPrefilled;


public class PackageTest {
	
	TestScenarioPrefilled scenario;
	
	@Before
	public void setup() {
		scenario = new TestScenarioPrefilled();
	}
	
	@Test
	public void datasupplyIsInstalledWhenVehicleReportsCorrectDataSupplyVersion() {
		assertFalse(isInstalledOnVehicle(scenario.package1));
		
		scenario.vehicle1.versions.setDataSupplyVersion(1, "1.0");
		
		assertTrue(isInstalledOnVehicle(scenario.package1));
	}

	@Test
	public void remoteCopyStickIsInstalledWhenPreviousTaskFinished() {
		Package copyStick = scenario.addPackage(PackageType.CopyStick, "1.1");
		assertFalse(isInstalledOnVehicle(copyStick));
		
		Task task = scenario.addTask(scenario.vehicle1, copyStick);
		assertFalse(isInstalledOnVehicle(copyStick));
		
		task.setClientStatus(ClientStage.FINISHED, 100);
		assertTrue(isInstalledOnVehicle(copyStick));
	}
	
	@Test
	public void multimediaContentIsInstalledWhenVehicleReportsCorrectContentVersion() {
		Package multimediaContent = scenario.addPackage(PackageType.Indis5MultimediaContent, "1.1");
		assertFalse(isInstalledOnVehicle(multimediaContent));
		
		scenario.vehicle1.versions.set(PackageType.Indis5MultimediaContent, "1.1");
		
		assertTrue(isInstalledOnVehicle(multimediaContent));
	}
	
	@Test
	public void xccEnnoSeatReservationIsInstalledWhenPreviousTaskFinished() {
		Package seatReservation = scenario.addPackage(PackageType.XccEnnoSeatReservation, "1.1");
		assertFalse(isInstalledOnVehicle(seatReservation));
		
		Task task = scenario.addTask(scenario.vehicle1, seatReservation);
		assertFalse(isInstalledOnVehicle(seatReservation));
		
		task.setClientStatus(ClientStage.FINISHED, 100);
		assertTrue(isInstalledOnVehicle(seatReservation));
	}
	
	private boolean isInstalledOnVehicle(Package pkg) {
		return pkg.isInstalledOn(scenario.vehicle1, scenario.taskRepository);
	}

}
