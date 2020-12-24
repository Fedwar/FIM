package fleetmanagement.backend.packages.importers;

import java.io.*;
import java.util.UUID;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;
import gsp.util.WrappedException;

public class Indis3MultimediaContentPackageImporter implements PackageImporter {

	@Override
	public boolean canImportPackage(String filename, File importDirectory) {
		if (!filename.endsWith(".zip"))
			return false;
		
		File md5File = PathUtil.findCaseInsensitive(importDirectory, "content/gsp.md5");
		return md5File.exists();
	}

	@Override
	public Package importPackage(String filename, File importDirectory) throws IOException {
		String version = readPackageVersion(importDirectory);
		return new Package(UUID.randomUUID(), PackageType.Indis3MultimediaContent, version,
				importDirectory, new PackageSize(importDirectory), null, null, null);
	}

    @Override
    public PackageType getPackageType() {
        return PackageType.Indis3MultimediaContent;
    }

    private String readPackageVersion(File importDirectory) throws FileNotFoundException, IOException {
		File mediaIndexXml = PathUtil.findCaseInsensitive(importDirectory, "content/mediaindex.xml");	
		return readVersionFromMediaIndex(mediaIndexXml);
	}

	private String readVersionFromMediaIndex(File mediaIndexXml) throws IOException {
		try {
			Document doc = new SAXBuilder().build(mediaIndexXml);
			return doc.getRootElement().getAttribute("version").getValue();
		} catch (JDOMException e) {
			throw new WrappedException(e);
		}
	}
}
