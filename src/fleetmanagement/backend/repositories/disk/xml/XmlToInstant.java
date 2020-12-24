package fleetmanagement.backend.repositories.disk.xml;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import gsp.util.DoNotObfuscate;

public class XmlToInstant extends XmlAdapter<String, Instant> {
	
	@DoNotObfuscate
	public XmlToInstant() { }

	@Override
	public String marshal(Instant v) throws Exception {
		return v == null ? null : DateTimeFormatter.ISO_INSTANT.format(v);
	}

	@Override
	public Instant unmarshal(String v) throws Exception {
		return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(v));
	}

}
