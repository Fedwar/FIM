package fleetmanagement.backend.repositories.disk.xml;

import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProtocolXmlFile {
	private static final Logger logger = Logger.getLogger(ProtocolXmlFile.class);
	private static final XmlSerializer serializer = new XmlSerializer(ProtocolXml.class);
	private File file;
	public ProtocolConfig[] protocolConfig;

	public ProtocolXmlFile(File directory) {
		this.file = new File(directory, "protocols.xml");
		this.protocolConfig = new ProtocolConfig[3];
		for (int i=0; i<3; i++)
			this.protocolConfig[i] = new ProtocolConfig();
		this.protocolConfig[0].enabled = true;
	}

	public void loadFromDisk() {
		logger.debug("Loading from disk: protocols");
		if (!file.exists())
			return;			// has nothing to do, defaults set in constructor

		ProtocolXml meta = (ProtocolXml)serializer.load(file);

		for (ProtocolConfigXml pc : meta.protocols) {
			this.protocolConfig[pc.index].enabled = pc.enabled;
		}
	}

	public void save() {
		ProtocolXml meta = new ProtocolXml();
	
		for (int i=0; i<3; i++) {
			ProtocolConfigXml xml = new ProtocolConfigXml();
			xml.index = i;
			xml.enabled = protocolConfig[i].enabled;
			meta.protocols.add(xml);
		}
		
		serializer.save(meta, file);
	}

	public class ProtocolConfig {
		public boolean enabled = false;
	}
	
	@XmlRootElement(name="protocolXml")
	private static class ProtocolXml {
		@XmlElementWrapper(name="protocols") @XmlElement(name="protocol") public List<ProtocolConfigXml> protocols = new ArrayList<>();
	}
	
	@DoNotObfuscate
	private static class ProtocolConfigXml {
		public int index;
		public boolean enabled;
	}
}
