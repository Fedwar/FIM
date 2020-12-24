package fleetmanagement.backend.packages.importers;

import java.io.*;
import java.util.UUID;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;
import gsp.util.*;

public class OebbDigitalContentPackageImporter implements PackageImporter {

	@Override
	public boolean canImportPackage(String filename, File importDirectory) {
		return filename.startsWith("OebbDigitalContent");
	}

	@Override
	public Package importPackage(String filename, File importDirectory) throws IOException {
		String version = readVersionFrom(new File(importDirectory, "version.xml"));
		return new Package(UUID.randomUUID(), PackageType.OebbDigitalContent, version, importDirectory,
				new PackageSize(importDirectory), null, null, null);
	}

    @Override
    public PackageType getPackageType() {
        return PackageType.OebbDigitalContent;
    }

    private String readVersionFrom(File versionFile) throws IOException {
		Document doc = parseXml(versionFile);
		Element version = XPathUtil.findElement("/version", doc);
		return version.getTextTrim();
	}

	private Document parseXml(File versionFile) throws IOException {
		try {
			SAXBuilder bld = new SAXBuilder();
			return bld.build(versionFile);
		}
		catch (JDOMException e) {
			throw new WrappedException(e);
		}
	}
}
