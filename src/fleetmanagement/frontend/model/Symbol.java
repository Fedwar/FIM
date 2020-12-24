package fleetmanagement.frontend.model;

import fleetmanagement.backend.packages.PackageType;

public class Symbol {
	public static String of(PackageType t) {
		switch (t) {
		case CopyStick:
			return "package-copystick.png";
			
		case Indis3MultimediaContent:
		case Indis5MultimediaContent:
		case PassengerTvContent:
			return "package-content.png";
			
		case DataSupply:
			return "package-data-supply.png";
			
		case XccEnnoSeatReservation:
		case SbhOutageTicker:
		default:
			return "package-generic.png";
		}
	}
}
