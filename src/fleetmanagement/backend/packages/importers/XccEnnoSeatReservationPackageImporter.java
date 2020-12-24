package fleetmanagement.backend.packages.importers;

import java.io.*;
import java.util.UUID;
import java.util.regex.*;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;

public class XccEnnoSeatReservationPackageImporter implements PackageImporter {

	private static final String FILENAME_PATTERN = "XccEnnoSitzplatz_(.+)\\.zip";

	@Override
	public boolean canImportPackage(String filename, File importDirectory) {
		return filename.matches(FILENAME_PATTERN);
	}

	@Override
	public Package importPackage(String filename, File importDirectory) throws IOException {
		Matcher matcher = Pattern.compile(FILENAME_PATTERN).matcher(filename);
		matcher.matches();
		String version = matcher.group(1);
		return new Package(UUID.randomUUID(), PackageType.XccEnnoSeatReservation, version,
				importDirectory, new PackageSize(importDirectory), null, null, null);
	}

    @Override
    public PackageType getPackageType() {
        return PackageType.XccEnnoSeatReservation;
    }

}
