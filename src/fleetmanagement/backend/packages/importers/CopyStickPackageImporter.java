package fleetmanagement.backend.packages.importers;

import java.io.*;
import java.util.UUID;
import java.util.regex.*;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;

public class CopyStickPackageImporter implements PackageImporter {

	private static final String FILENAME_PATTERN = "RemoteCopyStick_(.+)\\.zip";

	@Override
	public boolean canImportPackage(String filename, File importDirectory) {
		return filename.matches(FILENAME_PATTERN) && new File(importDirectory, "execute.cmd").exists();
	}

	@Override
	public Package importPackage(String filename, File importDirectory) throws IOException {
		Matcher matcher = Pattern.compile(FILENAME_PATTERN).matcher(filename);
		matcher.matches();
		String version = matcher.group(1);
		return new Package(UUID.randomUUID(), PackageType.CopyStick, version, importDirectory,
				new PackageSize(importDirectory), null, null, null);
	}

	@Override
	public PackageType getPackageType() {
		return PackageType.CopyStick;
	}

}
