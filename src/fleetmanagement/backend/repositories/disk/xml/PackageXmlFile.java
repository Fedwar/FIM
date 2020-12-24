package fleetmanagement.backend.repositories.disk.xml;

import fleetmanagement.backend.installations.PackageInstallation;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageSize;
import fleetmanagement.backend.packages.PackageType;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.UUID;

public class PackageXmlFile implements XmlFile<Package> {

	private static final Logger logger = Logger.getLogger(PackageXml.class);
	private static final XmlSerializer serializer = new XmlSerializer(PackageXml.class);
	private static final String fileName = "package.xml";
	private final File file;

	public PackageXmlFile(File packageDirectory) {
		this.file = new File(packageDirectory, fileName);
	}

	@Override
	public File file() {
		return file;
	}

	public void delete() {
		file.delete();
	}

	public boolean exists() {
		return file.exists();
	}

	public Package load() {
		try {
			if (exists()) {
				PackageXml xml = (PackageXml)serializer.load(file);
				Package result = new Package(xml.id, xml.type, xml.version, null, xml.size.toPackageSize(), xml.slot, xml.startOfPeriod, xml.endOfPeriod);
				result.source = xml.source;
				result.groupId = xml.groupId;
				result.archive = xml.fileName == null ? null : new File(xml.fileName);
				if (xml.packageInstallationId != null) {
					result.installation = new PackageInstallation(xml.packageInstallationId);
				}
				return result;
			}
		} catch (Exception e) {
			logger.error("Package in " + file.getParent() + " seems broken.", e);
		}
		return null;
	}
	
	public void save(Package p) {
		PackageXml meta = new PackageXml();
		meta.formatVersion = 1;
		meta.id = p.id;
		meta.type = p.type;
		meta.version = p.version;
		meta.slot = p.slot;
		meta.source = p.source;
		meta.size = new PackageSizeXml();
		meta.size.files = p.size.files;
		meta.size.bytes = p.size.bytes;
		meta.startOfPeriod = p.startOfPeriod;
		meta.endOfPeriod = p.endOfPeriod;
		meta.groupId = p.groupId;
		meta.fileName = p.archive == null ? null : p.archive.getName();
		meta.packageInstallationId = p.installation == null ? null
				: p.installation.id();
		
		serializer.save(meta, file);
	}

	@DoNotObfuscate
	@XmlRootElement(name="package")
	public static class PackageXml {
		@XmlAttribute(name="format-version") public int formatVersion;
		@XmlAttribute public UUID id;
		@XmlAttribute public PackageType type;
		@XmlAttribute public String version;
		@XmlAttribute public Integer slot;
		@XmlAttribute public String source;
		@XmlElement public PackageSizeXml size;
		@XmlAttribute public String startOfPeriod;
		@XmlAttribute public String endOfPeriod;
		@XmlAttribute public UUID groupId;
		@XmlAttribute public String fileName;
		@XmlAttribute public UUID packageInstallationId;
	}
	
	@DoNotObfuscate
	public static class PackageSizeXml {
		@XmlAttribute public int files;
		@XmlAttribute public int bytes;
		
		public PackageSize toPackageSize() {
			return new PackageSize(files, bytes);
		}
	}
}
