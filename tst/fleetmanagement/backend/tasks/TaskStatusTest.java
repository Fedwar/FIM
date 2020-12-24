package fleetmanagement.backend.tasks;

import static org.junit.Assert.*;

import org.junit.*;

import fleetmanagement.backend.tasks.TaskStatus;
import fleetmanagement.backend.tasks.TaskStatus.*;

public class TaskStatusTest {
	
	private TaskStatus tested;

	@Before
	public void setUp() {
		tested = new TaskStatus();
	}

	@Test
	public void initialStageIsPending() {
		assertEquals(TaskStatus.ClientStage.PENDING, tested.clientStage);
	}
	
	@Test
	public void supportsEquals() throws CloneNotSupportedException {
		assertNotEquals(tested, new TaskStatus(ServerStatus.Running, ClientStage.PENDING, 0));
		assertNotEquals(tested, new TaskStatus(ServerStatus.Pending, ClientStage.DOWNLOADING, 0));
		assertNotEquals(tested, new TaskStatus(ServerStatus.Pending, ClientStage.DOWNLOADING, 10));
	}
	
	@Test
	public void reportsCompletion() {
		assertFalse(new TaskStatus(ServerStatus.Pending, ClientStage.PENDING, 0).hasCompleted());
		assertFalse(new TaskStatus(ServerStatus.Pending, ClientStage.INITIALIZING, 0).hasCompleted());
		assertFalse(new TaskStatus(ServerStatus.Running, ClientStage.DOWNLOADING, 0).hasCompleted());
		assertTrue(new TaskStatus(ServerStatus.Failed, ClientStage.CANCELLED, 0).hasCompleted());
		assertTrue(new TaskStatus(ServerStatus.Failed, ClientStage.FAILED, 0).hasCompleted());
		assertTrue(new TaskStatus(ServerStatus.Cancelled, ClientStage.CANCELLED, 0).hasCompleted());
		assertTrue(new TaskStatus(ServerStatus.Finished, ClientStage.FINISHED, 0).hasCompleted());
	}
}
