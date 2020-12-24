package fleetmanagement.backend.packages.importers;

import java.io.*;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;

public interface PackageImporter {
	boolean canImportPackage(String filename, File importDirectory);
	Package importPackage(String filename, File importDirectory) throws IOException;
	PackageType getPackageType();
}
