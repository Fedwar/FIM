package fleetmanagement.backend.packages.importers;

import java.io.*;
import java.util.UUID;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;
import gsp.util.WrappedException;

public class Indis5MultimediaContentPackageImporter implements PackageImporter {

	@Override
	public boolean canImportPackage(String filename, File importDirectory) {
		if (!filename.endsWith(".zip"))
			return false;
		
		File md5File = PathUtil.findCaseInsensitive(importDirectory, "content/gsp.md5");
		File mediaIndexFile = tryFindMediaIndexFile(importDirectory);
		return !md5File.exists() && mediaIndexFile.exists();
	}

	@Override
	public Package importPackage(String filename, File importDirectory) throws IOException {
		String version = readPackageVersion(importDirectory);
		return new Package(UUID.randomUUID(), PackageType.Indis5MultimediaContent, version, importDirectory,
				new PackageSize(importDirectory), null, null, null);
	}

    @Override
    public PackageType getPackageType() {
        return PackageType.Indis5MultimediaContent;
    }

    private String readPackageVersion(File importDirectory) throws FileNotFoundException, IOException {
		return readVersionFromMediaIndex(tryFindMediaIndexFile(importDirectory));
	}

	private File tryFindMediaIndexFile(File importDirectory) {
		return PathUtil.findCaseInsensitive(importDirectory, "content/mediaindex.xml");
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
