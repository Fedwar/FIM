package fleetmanagement.backend.packages;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.repositories.Repository;

import java.util.List;
import java.util.UUID;

public interface PackageRepository extends Repository<Package, UUID> {

	List<Package> listByType(PackageType type);
	List<Package> listByGroupId(UUID groupId);
	Package duplicate(Package pkg, Group group);
	List<Package> getDuplicates(Package pkg);
	boolean isGroupContainsPackageDuplicate(Package pkg, Group group);
}