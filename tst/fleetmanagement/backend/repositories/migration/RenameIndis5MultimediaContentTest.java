package fleetmanagement.backend.repositories.migration;

import static org.junit.Assert.*;

import org.junit.Test;

import fleetmanagement.backend.repositories.migration.RenameIndis5MultimediaContent;
import fleetmanagement.backend.repositories.migration.DatabaseMigrations.DatabaseMigrationStep;

public class RenameIndis5MultimediaContentTest extends MigrationTest {
	
	@Override
	protected DatabaseMigrationStep createTested() {
		return new RenameIndis5MultimediaContent();
	}
	
	@Test
	public void renamesIndis5MultimediaContentType() throws Exception {
		String migrated = migrate("<package type=\"MultimediaContent\" />");
		assertEquals("<package type=\"Indis5MultimediaContent\" />", migrated);
	}
	
	@Test
	public void leavesOtherContentTypesUntouched() throws Exception {
		String migrated = migrate("<package type=\"DataSupply\" />");
		assertEquals("<package type=\"DataSupply\" />", migrated);
	}
}
