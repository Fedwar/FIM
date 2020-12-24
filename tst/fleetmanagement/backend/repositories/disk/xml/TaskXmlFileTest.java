package fleetmanagement.backend.repositories.disk.xml;

import static org.junit.Assert.*;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import fleetmanagement.TestFiles;
import fleetmanagement.backend.repositories.disk.xml.TaskXmlFile.TaskXml;
import fleetmanagement.backend.tasks.TaskStatus.ServerStatus;
import gsp.testutil.TemporaryDirectory;

public class TaskXmlFileTest {

	@Test
	public void setsCompletedTasksWithUnknownAbortionReasonToFailedDuringDataMigration() throws Exception {
		try (TemporaryDirectory tempDir = TemporaryDirectory.create()) {
			FileUtils.copyFileToDirectory(TestFiles.find("task-v1-migration/task.xml"), tempDir);
			TaskXmlFile file = new TaskXmlFile(tempDir);
			TaskXml task = file.meta();
			assertTrue(task.formatVersion > 1);
			assertEquals(ServerStatus.Failed, task.status.serverStatus);
		}
	}
}
