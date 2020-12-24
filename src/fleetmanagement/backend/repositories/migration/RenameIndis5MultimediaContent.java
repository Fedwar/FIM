package fleetmanagement.backend.repositories.migration;

import org.jdom2.*;

import fleetmanagement.backend.repositories.migration.DatabaseMigrations.DatabaseMigrationStep;

public class RenameIndis5MultimediaContent implements DatabaseMigrationStep {

	@Override
	public void migrate(Document xml) {
		Element rootElement = xml.getRootElement();
		if (rootElement.getAttributeValue("type").equals("MultimediaContent"))
			rootElement.setAttribute("type", "Indis5MultimediaContent");
	}

}
