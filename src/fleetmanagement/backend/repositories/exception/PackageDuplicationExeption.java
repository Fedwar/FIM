package fleetmanagement.backend.repositories.exception;

public class PackageDuplicationExeption extends RuntimeException {

	private static final long serialVersionUID = -1532999829563616163L;
	

	public PackageDuplicationExeption(String pkgVersion, String taskType) {
		super(String.format("Package %s of type '%s' already exists.", pkgVersion, taskType));
	}
}
