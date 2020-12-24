package fleetmanagement.backend.vehiclecommunication;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.time.ZonedDateTime;

import javax.ws.rs.WebApplicationException;

import fleetmanagement.backend.notifications.NotificationService;
import org.junit.*;

import com.sun.jersey.api.NotFoundException;

import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.vehiclecommunication.TasksResource.TaskList;
import fleetmanagement.test.TestScenarioPrefilled;
import gsp.testutil.Sleep;

public class TasksResourceTest {

	private TasksResource tested;
	private TestScenarioPrefilled scenario;
	private int taskPollingMillis = 10;
	private int longPollingTimeoutMillis = 100;

	@Before
	public void setUp() {
		scenario = new TestScenarioPrefilled();
		tested = new TasksResource(scenario.taskRepository, scenario.vehicleRepository, mock(NotificationService.class));
		tested.setLongPollingTimeoutMillis(longPollingTimeoutMillis);
		tested.setTaskPollingMillis(taskPollingMillis);
	}
	
	@Test (expected=NotFoundException.class)
	public void throwsExceptionWhenVehicleQueriesTaskWithUnknownId() throws InterruptedException {
		tested.getTaskById("067e6162-3b6f-4ae2-a171-2470b63dff00");
	}
	
	@Test (expected=WebApplicationException.class)
	public void throwsExceptionWhenQueryingWithUnknownUic() throws InterruptedException {
		tested.getTasks("unknown", false, null);
	}
	
	@Test
	public void returnsNextTaskOfVehicle() throws InterruptedException {
		Task task = scenario.addTask(scenario.vehicle1, scenario.package1);
		
		TaskList taskList = getTaskListWithoutLongpolling();
		
		assertEquals(task.getId().toString(), taskList.tasks.get(0).id);
	}
	
	@Test
	public void returnsEmptyTaskList() throws InterruptedException {
		TaskList taskList = getTaskListWithoutLongpolling();
		
		assertTrue(taskList.tasks.isEmpty());
	}
	
	@Test
	public void tasksListsHaveSameIdIfTheyAreUnchanged() throws InterruptedException {
		TaskList original = getTaskListWithoutLongpolling();
		TaskList unchanged = getTaskListWithoutLongpolling();
		
		assertEquals(original.id, unchanged.id);
	}
	
	@Test
	public void updatesVehicleLastSeen() throws InterruptedException {
		ZonedDateTime previousSastSeen = scenario.vehicle1.lastSeen = ZonedDateTime.parse("2015-01-30T12:30:00Z");
		
		getTaskListWithoutLongpolling();
		
		assertNotEquals(previousSastSeen, scenario.vehicle1.lastSeen);
	}
	
	@Test(timeout=100)
	public void allowsLongpollingForChangeOfNextTaskOnVehicle() throws InterruptedException {
		new Thread(() ->{
			Sleep.msecs(20);
			scenario.addTask(scenario.vehicle1, scenario.package2);
		}).start();
		
		TaskList original = getTaskListWithoutLongpolling();
		TaskList updated = getTaskListWithLongpolling(original.id);
		
		assertNotEquals(original.id, updated.id);
		assertEquals(1, updated.tasks.size());
	}

	private TaskList getTaskListWithoutLongpolling() throws InterruptedException {
		return tested.getTasks(scenario.vehicle1.uic, false, null);
	}
	
	private TaskList getTaskListWithLongpolling(String previousTasklistId) throws InterruptedException {
		return tested.getTasks(scenario.vehicle1.uic, true, previousTasklistId);
	}
	
	@Test(timeout=200)
	public void returnsUnchangedTasksAfterLongpollingTimeout() throws InterruptedException {
		long start = System.nanoTime();
		Task task = scenario.addTask(scenario.vehicle1, scenario.package1);
		TaskList original = getTaskListWithoutLongpolling();
		
		TaskList taskList = getTaskListWithLongpolling(original.id);
		
		assertTrue(System.nanoTime() - start > longPollingTimeoutMillis * 1E6);
		assertEquals(task.getId().toString(), taskList.tasks.get(0).id);
	}

}
