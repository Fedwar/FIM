package fleetmanagement.frontend.security.shiro;

import static org.junit.Assert.*;

import org.junit.Test;

public class ActiveDirectoryGroupTest {

	@Test
	public void comparesFullyQualifiedGroupNameAndShortName() {
		ActiveDirectoryGroup fullyQualified = new ActiveDirectoryGroup("CN=users,DC=gsp,DC=local");
		ActiveDirectoryGroup shortName = new ActiveDirectoryGroup("users");
		assertEquals(fullyQualified, shortName);
		assertEquals(fullyQualified.hashCode(), shortName.hashCode());
	}
	
	@Test
	public void returnsGroupNameAsString() {
		ActiveDirectoryGroup fullyQualified = new ActiveDirectoryGroup("CN=users,DC=gsp,DC=local");
		ActiveDirectoryGroup shortName = new ActiveDirectoryGroup("users");
		assertEquals("users", shortName.toString());
		assertEquals("CN=users,DC=gsp,DC=local", fullyQualified.toString());
	}
	
	@Test
	public void comparesFullyQualifiedGroupNamesIfPossible() {
		ActiveDirectoryGroup gspUsers = new ActiveDirectoryGroup("CN=users,DC=gsp,DC=local");
		ActiveDirectoryGroup googleUsers = new ActiveDirectoryGroup("CN=users,DC=google,DC=de");
		assertNotEquals(gspUsers, googleUsers);
	}
	
	@Test
	public void comparesShortNames() {
		ActiveDirectoryGroup users1 = new ActiveDirectoryGroup("users");
		ActiveDirectoryGroup users2 = new ActiveDirectoryGroup("users");
		assertEquals(users1, users2);
		assertEquals(users1.hashCode(), users2.hashCode());
	}
	
}
