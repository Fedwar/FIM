package fleetmanagement.frontend.security.shiro;

public class ActiveDirectoryGroup {
	private final String distinguishedName;
	private final String shortName;

	public ActiveDirectoryGroup(String name) {
		if (isDistinguishedName(name)) {
			this.distinguishedName = name;
			this.shortName = getShortName(name);
		} else {
			this.distinguishedName = null;
			this.shortName = name;
		}
	}

	private boolean isDistinguishedName(String name) {
		return name.startsWith("CN=");
	}

	private static String getShortName(String distinguishedName) {
		return distinguishedName.substring(3, distinguishedName.indexOf(','));
	}
	
	@Override
	public int hashCode() {
		return shortName.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		ActiveDirectoryGroup other = (ActiveDirectoryGroup)obj;
		if (this.distinguishedName != null && other.distinguishedName != null)
			return this.distinguishedName.equals(other.distinguishedName);

		return this.shortName.equals(other.shortName);
	}
	
	@Override
	public String toString() {
		return distinguishedName != null ? distinguishedName : shortName;
	}
}
