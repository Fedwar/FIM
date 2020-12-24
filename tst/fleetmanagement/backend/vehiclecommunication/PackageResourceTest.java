package fleetmanagement.backend.vehiclecommunication;

import com.sun.jersey.multipart.BodyPart;
import com.sun.jersey.multipart.MultiPart;
import com.sun.jersey.multipart.MultiPartMediaTypes;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.events.EventImpl;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageSize;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.memory.InMemoryTaskRepository;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.tasks.TaskStatus.ServerStatus;
import fleetmanagement.backend.vehiclecommunication.PackageResource.MetaFile;
import fleetmanagement.backend.vehiclecommunication.PackageResource.MetaItem;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PackageResourceTest {

	private PackageResource tested;
	private static Package pkg;
	private static Task task;
	private NotificationService notificationService;
	private TestScenario scenario;

	@BeforeClass
	public static void setup() throws Exception {
		File packageDir = TestFiles.find("sample-package");
		pkg = new Package(UUID.randomUUID(), PackageType.DataSupply, "1.0", packageDir, new PackageSize(0, 0),
				1, "08.09.2013 00:00:00","01.12.2013 23:59:59");
		task = new Task(pkg, new Vehicle("123", null, "vehicle",  "1.2.34567.0", ZonedDateTime.now(), null, false, 1));
	}

	@Before
	public void before() throws Exception {
		scenario = new TestScenario();
		notificationService = mock(NotificationService.class);
		scenario.taskRepository.insert(task);
		tested = new PackageResource(pkg, scenario.taskRepository, task.getId(), notificationService);
	}

	@Test
	public void servesFilesFromPackageDirectory() throws Exception {
		Response r = tested.deliverZipEntry("001/08200002.wav", null);
		InputStream is = (InputStream)r.getEntity();
		assertEquals(12522, is.available());
	}
	
	@Test
	public void continuesFilesDownloads() throws Exception {
		Response r = tested.deliverZipEntry("001/08200002.wav", "bytes=10000-");
		InputStream is = (InputStream)r.getEntity();
		assertEquals(2522, is.available());
	}
	
	@Test
	public void servesMetaFile() throws Exception {
		MetaFile md5 = tested.buildMetaFile();
		assertEquals(3, md5.files.size());
		MetaItem file1 = md5.files.get(0);
		assertEquals("001/08200002.wav", file1.name);
		assertEquals(12522, file1.size);
		assertEquals("de672dca3ccafca37efe0fc579ab9238c3d0aa1c187c31e795ca63e2f9bdb494", file1.sha256);
	}
	
	@Test
	public void servesMetaFileWithUTF8Filename() throws Exception {
		MetaFile md5 = tested.buildMetaFile();
		MetaItem file3 = md5.files.get(2);
		assertEquals("\u00E4\u00FC\u00DF \u044B\u0432\u0423\u0426/\u00F6\u041F\u0446.txt", file3.name);
	}
	
	@Test
	public void servesMultipleFiles() throws Exception {
		Response r = tested.deliverMultipleZipEntries("001/08200002.wav\r\nIBIS/IBIS_SYS/IBIS.ZNT");
		assertEquals(MultiPartMediaTypes.MULTIPART_MIXED_TYPE, r.getMetadata().get("Content-Type").get(0));
		
		MultiPart content = (MultiPart)r.getEntity();
		assertEquals(2, content.getBodyParts().size());

		BodyPart part = content.getBodyParts().get(0);
		assertEquals("12522", part.getHeaders().get("Content-Length").get(0));
		assertEquals("attachment; filename=\"001/08200002.wav\"", part.getHeaders().get("Content-Disposition").get(0));
		InputStream is = (InputStream)part.getEntity();
		assertEquals(12522, is.available());
	}
	
	@Test
	public void servesMultipleFilesWithUTF8Filename() throws Exception {
		Response r = tested.deliverMultipleZipEntries("\u00E4\u00FC\u00DF \u044B\u0432\u0423\u0426/\u00F6\u041F\u0446.txt\r\nIBIS/IBIS_SYS/IBIS.ZNT");
		assertEquals(MultiPartMediaTypes.MULTIPART_MIXED_TYPE, r.getMetadata().get("Content-Type").get(0));
		
		MultiPart content = (MultiPart)r.getEntity();
		assertEquals(2, content.getBodyParts().size());

		BodyPart part = content.getBodyParts().get(0);
		assertEquals("attachment; filename=\"\u00E4\u00FC\u00DF \u044B\u0432\u0423\u0426/\u00F6\u041F\u0446.txt\"", part.getHeaders().get("Content-Disposition").get(0));
	}
	
	@Test
	public void marksTaskAsRunningWhenDownloadStarts() throws Exception {
		tested.buildMetaFile();
		task = scenario.taskRepository.tryFindById(task.getId());
		assertEquals(ServerStatus.Running, task.getStatus().serverStatus);
		assertEquals(ClientStage.DOWNLOADING, task.getStatus().clientStage);
	}
	
	@Test(expected=FileNotFoundException.class)
	public void logsDownloadErrorsForSingleFiles() throws Exception {
		try {
			tested.deliverZipEntry("foo", null);
		}
		finally {
			task = scenario.taskRepository.tryFindById(task.getId());
			assertTrue(task.getLogMessages().stream().anyMatch(l -> l.severity == Severity.ERROR));
			verify(notificationService).processEvent(any(EventImpl.class));
		}
	}

	@Test(expected=FileNotFoundException.class)
	public void logsDownloadErrorsForMultiFileRequests() throws Exception {
		try {
			tested.deliverMultipleZipEntries("foo.txt");
		}
		finally {
			task = scenario.taskRepository.tryFindById(task.getId());
			assertTrue(task.getLogMessages().stream().anyMatch(l -> l.severity == Severity.ERROR));
			verify(notificationService).processEvent(any(Event.class));
		}
	}
}
