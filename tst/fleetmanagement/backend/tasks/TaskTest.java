package fleetmanagement.backend.tasks;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.tasks.TaskStatus.ServerStatus;
import fleetmanagement.backend.vehicles.Vehicle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static fleetmanagement.TestObjectFactory.createPackage;
import static fleetmanagement.TestObjectFactory.createVehicle;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TaskTest {

	private Package p = createPackage(PackageType.DataSupply, "v1");
	private Vehicle v = createVehicle("vehicle1");

	private Task tested;

	@Mock
	private Clock clock;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@Captor
	private ArgumentCaptor<ApplicationEvent> eventCaptor;

	@Before
	public void setUp() {
		when(clock.instant()).thenReturn(Instant.now());
		tested = new Task(UUID.fromString("067e6162-3b6f-4ae2-a171-2470b63dff00"), p, v.id,
				ZonedDateTime.parse("2015-01-30T12:30:00Z"),
				ZonedDateTime.parse("2015-12-31T12:30:00Z"),
				new TaskStatus(), new ArrayList<>(), clock, eventPublisher);
	}
	
	@Test
	public void initializesAllFields() {
		assertEquals(ZonedDateTime.parse("2015-01-30T12:30:00Z"), tested.getStartedAt());
		assertEquals(ZonedDateTime.parse("2015-12-31T12:30:00Z"), tested.getCompletedAt());
		assertEquals(UUID.fromString("067e6162-3b6f-4ae2-a171-2470b63dff00"), tested.getId());
		assertEquals(ClientStage.PENDING, tested.getStatus().clientStage);
		assertEquals(0, tested.getStatus().percent);
		assertEquals(ServerStatus.Pending, tested.getStatus().serverStatus);
		assertFalse(tested.isCancelled());
	}
	
	@Test
	public void logsStatusChanges() {
		tested.setClientStatus(ClientStage.DOWNLOADING, 10);
		assertTrue(tested.getLogMessages().stream().anyMatch(x -> x.message.contains("DOWNLOADING")));
	}
	
	@Test
	public void clientSideAbortsAreLoggedAsErrors() {
		tested.setClientStatus(ClientStage.CANCELLED, 0);
		assertTrue(hasLogsWith(Severity.ERROR));
	}
	
	@Test
	public void cancelledTasksAreLoggedAsWarnings() {
		tested.cancel();
		assertTrue(hasLogsWith(Severity.WARNING));
		
		tested.setClientStatus(ClientStage.CANCELLED, 0);
		assertFalse(hasLogsWith(Severity.ERROR));
	}

	@Test
	public void failedTasksReportedViaLegacyCancelledStatusAreLoggedAsErrors() {
		tested.setClientStatus(ClientStage.CANCELLED, 0);
		assertTrue(hasLogsWith(Severity.ERROR));
	}
	
	@Test
	public void failedTasksAreLoggedAsErrors() {
		tested.setClientStatus(ClientStage.FAILED, 0);
		assertTrue(hasLogsWith(Severity.ERROR));
	}
	
	@Test
	public void logsNoChangesIfStatusStaysTheSame() {
		tested.setClientStatus(ClientStage.DOWNLOADING, 10);
		int logCountBeforeSecondStatusChange = tested.getLogMessages().size();
		
		tested.setClientStatus(ClientStage.DOWNLOADING, 10);
		
		assertEquals(logCountBeforeSecondStatusChange, tested.getLogMessages().size());
	}
	
	@Test
	public void returnsCorrectJson() {
		TaskJson json = tested.getTaskJson();
		
		String id = tested.getId().toString();
		assertEquals(id, json.id);
		assertEquals("data-supply", json.type);
		assertEquals("/tasks/" + id + "/files", json.url);
	}
	
	@Test
	public void returnsFinishedAndCancelled() {
		assertFalse(tested.isCancelled());
		assertFalse(tested.isFinished());
		
		tested.setServerStatus(ServerStatus.Cancelled);
		
		assertNotNull(tested.getCompletedAt());
		assertTrue(tested.isCancelled());
		assertFalse(tested.isFinished());
				
		tested.setServerStatus(ServerStatus.Finished);
		
		assertFalse(tested.isCancelled());
		assertTrue(tested.isFinished());
	}
	
	@Test 
	public void setsCompletedDateWhenTaskFinished() throws IOException {
		tested.setServerStatus(ServerStatus.Finished);
		assertNotNull(tested.getCompletedAt());
	}
	
	@Test 
	public void setsCompletedDateOnlyOnce() throws IOException, InterruptedException {
		tested.cancel();
		ZonedDateTime completed = tested.getCompletedAt();
		
		Thread.sleep(100);
		tested.setClientStatus(ClientStage.CANCELLED, 10);
		
		assertEquals(completed, tested.getCompletedAt());
	}
	
	@Test 
	public void setsCompletedDateWhenTaskCancelled() throws IOException {
		tested.setServerStatus(ServerStatus.Cancelled);
		
		assertNotNull(tested.getCompletedAt());
	}
	
	@Test
	public void cannotEstimateTaskCompletionWhenTaskHasNotStartedYet() {
		assertNull(tested.getEstimatedCompletionDate());
	}
	
	@Test
	public void cannotEstimateTaskCompletionWhenTaskWasJustStarted() {
		Task t = simulateTaskCompletion(1, Duration.ofMinutes(2), 3);
		
		assertNull(t.getEstimatedCompletionDate());
	}
	
	@Test
	public void cannotEstimateTaskCompletionWhenTaskWasCancelled() {
		Task t =  simulateTaskCompletion(20, Duration.ofMinutes(15), 30);
		
		t.cancel();
		
		assertNull(t.getEstimatedCompletionDate());
	}
	
	@Test
	public void cannotEstimateTaskCompletionWhenTaskHasFinished() {
		Task t =  simulateTaskCompletion(20, Duration.ofMinutes(15), 30);
		t.setServerStatus(ServerStatus.Finished);
		
		assertNull(t.getEstimatedCompletionDate());
	}

	@Test
	public void cannotEstimateTaskCompletionBeforeSecondProgressWasReceived() {
		tested.setClientStatus(ClientStage.DOWNLOADING, 20);
		
		assertNull(tested.getEstimatedCompletionDate());
	}
	
	@Test
	public void estimatesTaskCompletionDate() {
		Task t =  simulateTaskCompletion(40, Duration.ofMinutes(11), 50);
		
		Instant completion = t.getEstimatedCompletionDate();
		
		Duration remaining = Duration.between(Instant.now(), completion);
		assertEquals(1, remaining.toHours());
	}

	@Test
	public void setsServerStatusBasedOnClientReports() {
		tested.setClientStatus(ClientStage.DOWNLOADING, 0);
		assertEquals(ServerStatus.Running, tested.getStatus().serverStatus);
	}
	
	@Test
	public void setsTaskFailedIfClientCancelsTask() {
		assertFalse(tested.isFailed());
		tested.setClientStatus(ClientStage.CANCELLED, 0);
		assertEquals(ServerStatus.Failed, tested.getStatus().serverStatus);
		assertTrue(tested.isFailed());
	}
	
	@Test
	public void staysCanceledIfClientAcknowledgesCancellation() {
		tested.setServerStatus(ServerStatus.Cancelled);
		tested.setClientStatus(ClientStage.CANCELLED, 0);
		assertEquals(ServerStatus.Cancelled, tested.getStatus().serverStatus);
	}
	
	@Test
	public void setsServerStatusToFinished() {
		tested.setClientStatus(ClientStage.FINISHED, 0);
		assertEquals(ServerStatus.Finished, tested.getStatus().serverStatus);
	}

	@Test
	public void setsServerStatusToFailed() {
		tested.setClientStatus(ClientStage.FAILED, 0);
		assertEquals(ServerStatus.Failed, tested.getStatus().serverStatus);
	}

	@Test
	public void sendsEvents_noneWhileInProgress() {
		tested.setServerStatus(ServerStatus.Pending);
		verifyZeroInteractions(eventPublisher);

		tested.setServerStatus(ServerStatus.Running);
		verifyZeroInteractions(eventPublisher);
	}

	@Test
	public void sendsEvents_onCancelled() {
		tested.setServerStatus(ServerStatus.Cancelled);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		verifyEvent(eventCaptor.getValue(), ServerStatus.Cancelled);
	}

	@Test
	public void sendsEvents_onFinished() {
		tested.setServerStatus(ServerStatus.Finished);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		verifyEvent(eventCaptor.getValue(), ServerStatus.Finished);
	}

	@Test
	public void sendsEvents_onFailed() {
		tested.setServerStatus(ServerStatus.Failed);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		verifyEvent(eventCaptor.getValue(), ServerStatus.Failed);
	}

	@Test
	public void noFailWithoutEventPublisher() {
		Task t = new Task(p, v, null);
		tested.setServerStatus(ServerStatus.Finished);
	}

	private boolean hasLogsWith(Severity severity) {
		return tested.getLogMessages().stream().anyMatch(x -> x.severity == severity);
	}

	private Task simulateTaskCompletion(int initialPercent, Duration timeTaken, int newPercent) {
		Task t = new Task(UUID.randomUUID(), p, v.id, ZonedDateTime.now().minus(timeTaken), null,
				new TaskStatus(), new ArrayList<>(), clock, eventPublisher);
		
		Instant start = Instant.now();		
		when(clock.instant()).thenReturn(start);
		t.setClientStatus(ClientStage.DOWNLOADING, initialPercent);
		when(clock.instant()).thenReturn(start.plus(timeTaken));
		t.setClientStatus(ClientStage.DOWNLOADING, newPercent);
		return t;
	}

	private void verifyEvent(ApplicationEvent event, ServerStatus serverStatus) {
		assertThat(event, instanceOf(TaskCompleteEvent.class));
		TaskCompleteEvent e = (TaskCompleteEvent) event;
		assertThat(e.getTask(), sameInstance(tested));
		assertThat(e.getTask().getStatus().serverStatus, is(serverStatus));
	}
}
