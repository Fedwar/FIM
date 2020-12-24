package fleetmanagement.backend.repositories.exception;

import fleetmanagement.backend.packages.PackageType;

public class PackageTypeNotLicenced extends PackageImportException {

	private static final long serialVersionUID = -1532999829563616163L;

	public PackageTypeNotLicenced(PackageType packageType) {
		super(packageType, "licence_package_type_disabled", packageType);
	}
}
