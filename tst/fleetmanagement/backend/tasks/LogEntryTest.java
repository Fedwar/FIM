package fleetmanagement.backend.tasks;

import static org.junit.Assert.*;

import java.time.Instant;

import org.junit.*;

import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.LogEntry.Severity;


public class LogEntryTest {
	
	private LogEntry tested;
	private Instant now;
	
	@Before
	public void setup() {
		now = Instant.parse("2016-02-29T14:28:41.010Z");
		tested = new LogEntry(now, Severity.WARNING, "Hallo Welt");
	}
	
	@Test
	public void implementsEquals() {
		LogEntry equal = new LogEntry(tested.time, Severity.WARNING, "Hallo Welt");
		assertEquals(equal, tested);
		assertEquals(equal.hashCode(), tested.hashCode());
	}
	
	@Test
	public void implementsToString() {
		assertEquals("[2016-02-29T14:28:41.010Z] WARNING Hallo Welt", tested.toString());
	}
}
