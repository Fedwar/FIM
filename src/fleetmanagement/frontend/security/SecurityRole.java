package fleetmanagement.frontend.security;

import gsp.util.DoNotObfuscate;

import java.util.*;
import java.util.stream.Collectors;

@DoNotObfuscate
public enum SecurityRole {
	Viewer, User, Config, Admin;
	
	public static SecurityRole parse(String name) {
		for (SecurityRole role : SecurityRole.values()) {
			if (role.name().equalsIgnoreCase(name))
				return role;
		}
		
		throw new RuntimeException("Not a valid user role: " + name);
	}
	
	public static List<SecurityRole> parseRoleList(String roles) {
		String[] splitRoles = roles.split(",");
		return Arrays.stream(splitRoles).map(SecurityRole::parse).collect(Collectors.toList());
	}
}
