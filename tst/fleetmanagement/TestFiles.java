package fleetmanagement;

import gsp.configuration.LocalFiles;

import java.io.File;

public class TestFiles {
	public static File find(String path) {
		return LocalFiles.find(path);
	}
}
