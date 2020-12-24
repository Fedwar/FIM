package fleetmanagement.backend.repositories.exception;

import fleetmanagement.backend.packages.PackageType;

public class PackageImportException extends LocalizableException {

	private static final long serialVersionUID = -1532999829563616163L;
	PackageType packageType;

	public PackageImportException(PackageType packageType, String message, Object... params) {
		super(message, params);
		this.packageType = packageType;
	}

	public PackageImportException(PackageType packageType, Throwable cause, String message, Object... params) {
		super(cause, message, params);
		this.packageType = packageType;
	}

	public PackageType getPackageType() {
		return packageType;
	}
}
