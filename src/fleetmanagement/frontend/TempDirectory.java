package fleetmanagement.frontend;

import java.io.*;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class TempDirectory {
	
	private static final Logger logger = Logger.getLogger(TempDirectory.class);
	
	private final File tempDirectory;
	
	public TempDirectory(File dataDirectory) {
		this.tempDirectory = new File(dataDirectory, "temp");
		tempDirectory.mkdirs();
	}

	public File getPath(String relativePath) {
		return new File(tempDirectory, relativePath);
	}
	
	public void clean() throws IOException {
		try {
			FileUtils.cleanDirectory(tempDirectory);
		} catch (Exception e) {
			logger.warn("Failed to delete contents of temp dir: ", e);
		}
	}
}
