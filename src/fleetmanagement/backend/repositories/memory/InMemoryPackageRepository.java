package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.repositories.disk.OnDiskPackageRepository;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.config.Licence;

import java.io.File;
import java.util.UUID;

public class InMemoryPackageRepository extends OnDiskPackageRepository {

	public InMemoryPackageRepository(TaskRepository tasks, Licence licence) {
		super(null, tasks, licence);
	}

	@Override
	public void loadFromDisk() {}

	@Override
	protected File getDirectory(Package persistable) {
		return null;
	}

	@Override
	protected void persist(Package persistable) {}

	@Override
	protected void assignFilesSubDir(Package pkg) {}

	@Override
	public Package duplicate(Package pkg, Group group) {
		UUID copyUuid = UUID.randomUUID();
		Package copy = new Package(
				copyUuid,
				pkg.type,
				pkg.version,
				null,
				pkg.size,
				pkg.slot,
				pkg.startOfPeriod,
				pkg.endOfPeriod
		);
		copy.groupId = group == null ? null : group.id;
		insert(copy);
		return copy;
	}
}
