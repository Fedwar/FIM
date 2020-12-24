package fleetmanagement.backend.repositories.disk.xml;

import java.time.ZonedDateTime;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import gsp.util.DoNotObfuscate;

@DoNotObfuscate
public class XmlToZonedDateTime extends XmlAdapter<String, ZonedDateTime> {

	public XmlToZonedDateTime() { }

	@Override
	public String marshal(ZonedDateTime v) throws Exception {
		return v == null ? null : v.toString();
	}

	@Override
	public ZonedDateTime unmarshal(String v) throws Exception {
		return ZonedDateTime.parse(v);
	}

}
