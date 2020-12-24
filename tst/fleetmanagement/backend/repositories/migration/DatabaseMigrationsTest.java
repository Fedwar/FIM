package fleetmanagement.backend.repositories.migration;

import static org.junit.Assert.*;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.*;

import fleetmanagement.backend.repositories.migration.DatabaseMigrations;
import gsp.testutil.TemporaryDirectory;

public class DatabaseMigrationsTest {
	private DatabaseMigrations tested;
	private TemporaryDirectory databaseDir;
	
	@Before
	public void setup() {
		databaseDir = TemporaryDirectory.create();
		tested = new DatabaseMigrations();
	}
	
	@After
	public void teardown() {
		databaseDir.delete();
	}
	
	@Test
	public void callsMigrationStepForEachFileInDirectory() throws Exception {
		File database = databaseDir.append("database.xml");
		FileUtils.write(database, "<legacy value=\"1\" />");
		tested.addMigrationStep(d -> d.getRootElement().setName("migrated"));
		
		runMigrations();
		
		String migrated = FileUtils.readFileToString(database);
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><migrated value=\"1\" />", migrated);
	}

	private void runMigrations() throws Exception {
		tested.performMigrations(databaseDir, "database.xml");
	}
	
}
