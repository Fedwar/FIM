package fleetmanagement.backend.repositories.migration;

import org.jdom2.*;

import fleetmanagement.backend.repositories.migration.DatabaseMigrations.DatabaseMigrationStep;

public class AddMissingPackageSource implements DatabaseMigrationStep {

	@Override
	public void migrate(Document xml) {
		Element rootElement = xml.getRootElement();
		if (rootElement.getAttribute("source") == null)
			rootElement.setAttribute("source", "User: Unknown");
	}

}
