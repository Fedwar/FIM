package fleetmanagement.backend.vehiclecommunication.upload;

import fleetmanagement.backend.events.EventImpl;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.vehiclecommunication.upload.TaskLogEntryUploadListener.UnknownTask;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.Charsets;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TaskLogEntryUploadListenerTest {
	
	private TaskLogEntryUploadListener tested;

	private NotificationService notificationService;
	private TestScenarioPrefilled scenario;
	private Task task;
	
	@Before
	public void setup() throws Exception {
		scenario = new TestScenarioPrefilled();
		notificationService = mock(NotificationService.class);
		task = scenario.addTask(scenario.vehicle1, scenario.package1);
		tested = new TaskLogEntryUploadListener(scenario.taskRepository, notificationService);
	}
	
	@Test
	public void writesReceivedLogsIntoTaskLog() {
		uploadJson("{ \"message\": \"Hello World\", \"severity\": \"info\" }");
		
		LogEntry logged = getLastLogMessage(task);
		
		assertEquals("Hello World", logged.message);
		assertEquals(LogEntry.Severity.INFO, logged.severity);
	}
	
	@Test
	public void recognizesErrorMessages() {
		uploadJson("{ \"message\": \"something went wrong\", \"severity\": \"error\" }");
		
		LogEntry logged = getLastLogMessage(task);
		
		assertEquals(LogEntry.Severity.ERROR, logged.severity);
	}

	@Test
	public void recognizesWarningMessages() {
		uploadJson("{ \"message\": \"something went wrong\", \"severity\": \"warning\" }");
		
		LogEntry logged = getLastLogMessage(task);
		
		assertEquals(LogEntry.Severity.WARNING, logged.severity);
	}

	@Test
	public void ignoresUnknownSeverity() {
		uploadJson("{ \"message\": \"Hello\", \"severity\": \"unknown\" }");
		
		LogEntry logged = getLastLogMessage(task);
		
		assertEquals(LogEntry.Severity.INFO, logged.severity);
	}

	@Test
	public void ignoresMissingSeverity() {
		uploadJson("{ \"message\": \"Hello\" }");
		
		LogEntry logged = getLastLogMessage(task);
		
		assertEquals(LogEntry.Severity.INFO, logged.severity);
	}
	
	@Test(expected=UnknownTask.class)
	public void throwsExceptionForLogEntryWithUnknownTask() {
		String validJson = "{ \"message\": \"Hello World\", \"severity\": \"info\" }";

		tested.onFileUploaded(scenario.vehicle1.id, "task-log_123e4567-e89b-12d3-a456-426655440000_1.json", validJson.getBytes(Charsets.UTF_8));
	}
	
	@Test
	public void acceptsLogEntryFiles() {
		assertTrue(tested.canHandleUploadedFile("task-log_123e4567-e89b-12d3-a456-426655440000_1.json"));
		assertTrue(tested.canHandleUploadedFile("task-log_123e4567-e89b-12d3-a456-426655440000.json"));
	}

	@Test
	public void rejectsNonLogEntryFiles() {
		assertFalse(tested.canHandleUploadedFile("task-status_123e4567-e89b-12d3-a456-426655440000.json"));
	}

	@Test
	public void triggersNotificationEvent() {
		uploadJson("{ \"message\": \"Hello\" }");
		verify(notificationService).processEvent(any(EventImpl.class));
	}

	private LogEntry getLastLogMessage(Task t) {
		return t.getLogMessages().get(task.getLogMessages().size() - 1);
	}
	
	private void uploadJson(String json) {
		String filename = "task-log_" + task.getId() + "_1.json";
		tested.onFileUploaded(scenario.vehicle1.id, filename, json.getBytes(Charset.forName("UTF-8")));
	}
}
