package fleetmanagement.frontend.security.shiro;

import java.util.*;

import fleetmanagement.frontend.security.SecurityRole;

public class GroupRoleMapping {
	
	private final Map<ActiveDirectoryGroup, List<SecurityRole>> mappings = new HashMap<>();
	
	public void add(ActiveDirectoryGroup group, SecurityRole role) {
		List<SecurityRole> roles = mappings.computeIfAbsent(group, x -> new ArrayList<>());
		roles.add(role);
	}
	
	public List<SecurityRole> getRolesForGroup(ActiveDirectoryGroup group) {
		return mappings.getOrDefault(group, Collections.emptyList());
	}
}
