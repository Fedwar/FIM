package fleetmanagement.frontend.security.shiro;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class ActiveDirectoryDomain {

	public static final ActiveDirectoryDomain NO_DOMAIN = new ActiveDirectoryDomain("");
	
	private final String domainSuffix;

	public ActiveDirectoryDomain(String domainSuffix) {
		if (domainSuffix.startsWith("@"))
			domainSuffix = domainSuffix.substring(1);
		
		this.domainSuffix = domainSuffix;
	}

	public String qualify(String username) {
		if (!domainSuffix.isEmpty() && !StringUtils.endsWithIgnoreCase(username, domainSuffix))
			return username + "@" + domainSuffix;
		
		return username;
	}
    
    public String toLdapSearchBase() {
    	return Arrays.stream(StringUtils.split(domainSuffix, ".")).map(x -> "DC=" + x).collect(Collectors.joining(","));
    }
    
    @Override
    public String toString() {
    	return domainSuffix.isEmpty() ? "<no domain>" : domainSuffix;
    }
    
    @Override
    public int hashCode() {
    	return domainSuffix.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
    	ActiveDirectoryDomain other = (ActiveDirectoryDomain)obj;
    	return this.domainSuffix.equals(other.domainSuffix);
    }
}
