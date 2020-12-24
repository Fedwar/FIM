package fleetmanagement.backend.repositories.migration;

import org.jdom2.*;
import org.jdom2.xpath.XPathExpression;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.migration.DatabaseMigrations.DatabaseMigrationStep;
import gsp.util.XPathUtil;

public class DistinguishBetweenIndis3AndIndis5MultimediaContent implements DatabaseMigrationStep {

	@Override
	public void migrate(Document xml) {
		String vehicleName = XPathUtil.findAttributeValue("/vehicle/@name", xml);
		XPathExpression<Element> xpath = XPathUtil.compileElementXPath("/vehicle/versions/version/component[text()='MULTIMEDIA_CONTENT']");
		for (Element toMigrate : xpath.evaluate(xml)) {
			toMigrate.setText(guessTypeFromVehicleName(vehicleName).toString());
		}
	}
	
	public static PackageType guessTypeFromVehicleName(String vehicleName) {
		if (vehicleName.startsWith("FAL"))
			return PackageType.Indis3MultimediaContent;
		
		return PackageType.Indis5MultimediaContent;
	}

}
