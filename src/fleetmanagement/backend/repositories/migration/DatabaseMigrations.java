package fleetmanagement.backend.repositories.migration;

import java.io.*;
import java.util.*;

import fleetmanagement.backend.Backend;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;
import org.apache.log4j.Logger;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.*;

import gsp.util.WrappedException;

public class DatabaseMigrations {
	private static final Logger logger = Logger.getLogger(DatabaseMigrations.class);


	public interface DatabaseMigrationStep {
		public void migrate(Document xml);
	}

	private final List<DatabaseMigrationStep> migrationSteps = new ArrayList<>();
	
	public void addMigrationStep(DatabaseMigrationStep step) {
		migrationSteps.add(step);
	}
	
	public void performMigrations(File directory, String filename) {
		try {
			migrateThrowingExceptions(directory, filename);
		}
		catch (IOException | JDOMException e) {
			throw new WrappedException(e);
		}
	}


	private void migrateThrowingExceptions(File directory, String filename) throws JDOMException, IOException {
		Collection<File> files = FileUtils.listFiles(directory, FileFilterUtils.nameFileFilter(filename), TrueFileFilter.INSTANCE);
		for (File file : files) {
			if (file.length() > 0)
				performMigrations(file);
		}
	}

	private void performMigrations(File file) throws JDOMException, IOException {
		Document doc = load(file);
		for (DatabaseMigrationStep step : migrationSteps) {
			step.migrate(doc);
		}
		save(doc, file);
	}

	private Document load(File file) throws JDOMException, IOException {
		return new SAXBuilder().build(file);
	}

	private void save(Document doc, File file) throws IOException {
		try(FileOutputStream os = new FileOutputStream(file)) {
			new XMLOutputter(Format.getCompactFormat().setLineSeparator(LineSeparator.NONE)).output(doc, os);
		}
	}
}
