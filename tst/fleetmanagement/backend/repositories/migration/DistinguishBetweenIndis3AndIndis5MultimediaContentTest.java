package fleetmanagement.backend.repositories.migration;

import static org.junit.Assert.*;

import org.junit.Test;

import fleetmanagement.backend.repositories.migration.DatabaseMigrations.DatabaseMigrationStep;

public class DistinguishBetweenIndis3AndIndis5MultimediaContentTest extends MigrationTest {
	
	@Override
	protected DatabaseMigrationStep createTested() {
		return new DistinguishBetweenIndis3AndIndis5MultimediaContent();
	}
	
	@Test
	public void detectsIndis3MultimediaBasedOnVehicleName() throws Exception {
		String migrated = migrate("<vehicle name=\"FAL 123\"><versions><version><component>MULTIMEDIA_CONTENT</component></version></versions></vehicle>");
		assertEquals("<vehicle name=\"FAL 123\"><versions><version><component>Indis3MultimediaContent</component></version></versions></vehicle>", migrated);
	}
	
	@Test
	public void detectsIndis5MultimediaBasedOnVehicleName() throws Exception {
		String migrated = migrate("<vehicle name=\"DISA 123\"><versions><version><component>MULTIMEDIA_CONTENT</component></version></versions></vehicle>");
		assertEquals("<vehicle name=\"DISA 123\"><versions><version><component>Indis5MultimediaContent</component></version></versions></vehicle>", migrated);
	}
	
	@Test
	public void doesNotTouchOtherComponentTypes() throws Exception {
		String migrated = migrate("<vehicle name=\"DISA 123\"><versions><version><component>CopyStick</component></version></versions></vehicle>");
		assertEquals("<vehicle name=\"DISA 123\"><versions><version><component>CopyStick</component></version></versions></vehicle>", migrated);
	}
}
