package fleetmanagement.backend.repositories.migration;

import java.io.StringReader;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.*;
import org.junit.Before;

import fleetmanagement.backend.repositories.migration.DatabaseMigrations.DatabaseMigrationStep;

public abstract class MigrationTest {
	
	protected DatabaseMigrationStep tested;
	
	@Before
	public void setup() {
		tested = createTested();
	}
	
	protected abstract DatabaseMigrationStep createTested();

	protected String migrate(String string) throws Exception {
		Document doc = new SAXBuilder().build(new StringReader(string));
		tested.migrate(doc);
		return new XMLOutputter(Format.getCompactFormat().setLineSeparator(LineSeparator.NONE).setOmitDeclaration(true)).outputString(doc);		
	}
}
