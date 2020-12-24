package fleetmanagement.backend.packages.importers;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;

public class PassengerTvContentPackageImporter implements PackageImporter {

	@Override
	public boolean canImportPackage(String filename, File importDirectory) {
		filename = filename.toLowerCase(Locale.ROOT);
		if (!filename.startsWith("fahrgast-tv-") || !filename.endsWith(".zip"))
			return false;
		
		File versionTxt = tryFindVersionText(importDirectory);
		return versionTxt.exists();
	}

	@Override
	public Package importPackage(String filename, File importDirectory) throws IOException {
		String version = readPackageVersion(importDirectory);
		return new Package(UUID.randomUUID(), PackageType.PassengerTvContent, version, importDirectory,
				new PackageSize(importDirectory), null, null, null);
	}

    @Override
    public PackageType getPackageType() {
        return PackageType.PassengerTvContent;
    }

    private String readPackageVersion(File importDirectory) throws IOException {
		File versionTxt = tryFindVersionText(importDirectory);
		return FileUtils.readLines(versionTxt).get(0);
	}
	
	private File tryFindVersionText(File importDirectory) {
		return PathUtil.findCaseInsensitive(importDirectory, "version.txt");
	}
}
