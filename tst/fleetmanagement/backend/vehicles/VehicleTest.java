package fleetmanagement.backend.vehicles;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.*;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus.*;
import fleetmanagement.backend.vehicles.DiagnosticSummary.DiagnosticSummaryType;
import fleetmanagement.test.TestScenarioPrefilled;


public class VehicleTest {
	private TestScenarioPrefilled scenario;
	private Vehicle tested;
	private Task task2;
	private Task task1;
	private Task task3;
	
	@Before
	public void setup() {
		scenario = new TestScenarioPrefilled();
		tested = scenario.vehicle1;
		
		task1 = scenario.addTask(scenario.vehicle1, scenario.package1);
		task2 = scenario.addTask(scenario.vehicle1, scenario.package2);
		task3 = scenario.addTask(scenario.vehicle1, scenario.addPackage(PackageType.CopyStick, "dummy"));
	}
	
	@Test
	public void signalsMissingCommunicationInDiagnosisSummary() {
		ZonedDateTime now = ZonedDateTime.now();
		simulateMissingCommunication(now);
		
		assertEquals(DiagnosticSummaryType.NotSeen, getDiagnosticSummary(now).type);
	}
	
	@Test
	public void listsRunningTasksInOrderOfCreation() {
		task2.cancel();
		
		List<Task> runningTasks = tested.getRunningTasks(scenario.taskRepository);
		
		assertEquals(task1, runningTasks.get(0));
		assertEquals(task3, runningTasks.get(1));		
	}
	
	@Test
	public void returnsOldestRunningTasksAsNextTask() {
		assertEquals(task1, getNextTask());
		
		task1.setClientStatus(ClientStage.FINISHED,  100);
		assertEquals(task2, getNextTask());
		
		task2.setServerStatus(ServerStatus.Cancelled);
		assertEquals(task3, getNextTask());
		
		task3.setClientStatus(ClientStage.CANCELLED, 0);
		assertEquals(null, getNextTask());		
	}

	private Task getNextTask() {
		return tested.getNextTask(scenario.taskRepository);
	}

	private DiagnosticSummary getDiagnosticSummary(ZonedDateTime now) {
		return scenario.vehicle1.getDiagnosticSummary(now);
	}

	private void simulateMissingCommunication(ZonedDateTime now) {
		scenario.vehicle1.lastSeen = now.minus(3, ChronoUnit.DAYS);
	}
}
