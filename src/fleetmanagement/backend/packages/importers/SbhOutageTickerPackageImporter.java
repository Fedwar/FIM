package fleetmanagement.backend.packages.importers;

import java.io.*;
import java.util.*;

import org.jdom2.Element;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;
import gsp.util.XmlUtil;

public class SbhOutageTickerPackageImporter implements PackageImporter {

	@Override
	public boolean canImportPackage(String filename, File importDirectory) {
		filename = filename.toLowerCase(Locale.ROOT);
		return filename.startsWith("et474-outage-ticker") && filename.endsWith(".zip");
	}

	@Override
	public Package importPackage(String filename, File importDirectory) throws IOException {
		String version = readPackageVersion(importDirectory);
		return new Package(UUID.randomUUID(), PackageType.SbhOutageTicker, version, importDirectory, new PackageSize(importDirectory), null, null, null);
	}

    @Override
    public PackageType getPackageType() {
        return PackageType.SbhOutageTicker;
    }

    private String readPackageVersion(File importDirectory) throws IOException {
		File tickerXml = tryFindOutageTickerXml(importDirectory);
		Element parsed = XmlUtil.parse(tickerXml);
		String version = parsed.getAttributeValue("version");
		
		if (version == null)
			throw new RuntimeException("Missing package version");
		
		return version;
	}
	
	private File tryFindOutageTickerXml(File importDirectory) {
		return PathUtil.findCaseInsensitive(importDirectory, "ticker.xml");
	}
}
