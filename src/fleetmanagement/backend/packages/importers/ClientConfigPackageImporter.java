package fleetmanagement.backend.packages.importers;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageSize;
import fleetmanagement.backend.packages.PackageType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientConfigPackageImporter implements PackageImporter {

    private static final String FILENAME_PATTERN = "ClientConfig_(.+)\\.zip";

    @Override
    public boolean canImportPackage(String filename, File importDirectory) {
        return filename.matches(FILENAME_PATTERN) && new File(importDirectory, "update_client.ini").exists();
    }

    @Override
    public Package importPackage(String filename, File importDirectory) throws IOException {
        Matcher matcher = Pattern.compile(FILENAME_PATTERN).matcher(filename);
        if (!matcher.matches())
            throw new FileNotFoundException("Client config package file name doesn't match expected pattern!");
        String version = matcher.group(1);
        return new Package(UUID.randomUUID(), PackageType.ClientConfig, version, importDirectory,
                new PackageSize(importDirectory), null, null, null);
    }

    @Override
    public PackageType getPackageType() {
        return PackageType.ClientConfig;
    }
}
