package fleetmanagement.backend.tasks;

import static org.junit.Assert.*;

import org.junit.*;

import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.test.TestScenarioPrefilled;

public class TaskLogFileTest {

	private TaskLogFile tested;
	private Task task;

	@Before
	public void setup() throws Exception {
		TestScenarioPrefilled scenario = new TestScenarioPrefilled();
		task = new Task(scenario.package1, scenario.vehicle1);
		tested = new TaskLogFile(task);
	}
	
	@Test
	public void returnsAllLogsAsString() {		
		task.addLog(new LogEntry(Severity.INFO, "log1"));
		task.addLog(new LogEntry(Severity.WARNING, "log2"));
		
		String allLogs = tested.getContent();
		
		String[] lines = allLogs.split("\r\n");
		assertTrue(lines[0].endsWith("INFO log1"));
		assertTrue(lines[1].endsWith("WARNING log2"));
	}
	
	@Test
	public void proposesFilename() {
		assertEquals("task-" + task.getId() + "-log.txt", tested.getFilename());
	}
}