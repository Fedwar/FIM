package fleetmanagement.frontend;

import static org.junit.Assert.*;

import java.io.*;

import org.junit.*;

import gsp.testutil.TemporaryDirectory;

public class TempDirectoryTest {

	private TempDirectory tested;
	private TemporaryDirectory testDir;
	
	@Before
	public void setUp() throws Exception {
		testDir = TemporaryDirectory.create();
		tested = new TempDirectory(testDir);
	}
	
	@After
	public void tearDown() {
		testDir.delete();
	}
	
	@Test
	public void createsTempDir() {
		assertTrue(new File(testDir, "temp").isDirectory());
	}
	
	@Test
	public void smoothlyHandlesExistingTempDir() {
		Assume.assumeTrue(new File(testDir, "temp").isDirectory());
		new TempDirectory(testDir);
	}
	
	@Test
	public void convertsRelativeToAbsolutePaths() {
		File expectedPath = new File(testDir, "temp" + File.separator + "foo");
		assertEquals(expectedPath, tested.getPath("foo"));
	}
	
	@Test
	public void cleansTempDirectoryContents() throws Exception {
		File fileInTempDir = createTempFile("file.dat");
		File fileInFolderInTempDir = createTempFile("folder" + File.separator + "file.dat");
		File folderInTempDir = new File(testDir, "temp" + File.separator + "folder");
		
		tested.clean();
		
		assertFalse(fileInTempDir.exists());
		assertFalse(folderInTempDir.isDirectory());
		assertFalse(fileInFolderInTempDir.exists());
	}

	@Test
	public void cleaningFailsSilently() throws Exception {
		File fileInTempDir = createTempFile("file.dat");
		
		try (FileInputStream in = new FileInputStream(fileInTempDir)) { //ensure file is in use
			tested.clean();
		}	
	}

	private File createTempFile(String relativePath) throws IOException {
		File created = new File(testDir, "temp" + File.separator + relativePath);
		File parent = created.getParentFile();
		parent.mkdirs();
		created.createNewFile();
		return created;
	}
	
}
