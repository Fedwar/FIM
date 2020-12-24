package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.installations.PackageInstallation;
import fleetmanagement.backend.installations.PackageInstallationRepository;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.exception.PackageTypeNotLicenced;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskCompleteEvent;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.tasks.TaskStatus;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.config.Licence;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static fleetmanagement.TestObjectFactory.createPackage;
import static fleetmanagement.TestObjectFactory.createVehicle;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OnDiskPackageRepositoryTest {

	private static final String PACKAGE_FILE_NAME = "test.file";
	private static final String PACKAGE_ARCHIVE = "Nearly_Empty_DV.zip";
	
	private OnDiskPackageRepository toWrite;
	private OnDiskPackageRepository toRead;
	@Mock
	private Licence licence;
	@Mock
	private PackageInstallationRepository packageInstallationRepository;

	private Package p = createPackage(PackageType.DataSupply, "v1", 1, "2020-01-01", "2020-10-01");
	private Vehicle v = createVehicle("vehicle1");
	private File archive;

	@Rule
	public TempFileRule repositoryDir = new TempFileRule();
	@Rule
	public TempFileRule importDir = new TempFileRule();
	@Rule
	public TempFileRule uploadDir = new TempFileRule();
	@Mock
	private TaskRepository taskRepository;


	@Before
	public void setup() throws IOException {
		addPackageFiles();
		when(licence.isPackageTypeAvailable(any())).thenReturn(true);
		toWrite = makeRepository();
		toRead = makeRepository();
	}

	private void addPackageFiles() throws IOException {
		p.path = importDir;
		archive = new File(uploadDir, PACKAGE_ARCHIVE);
		FileUtils.writeLines(new File(importDir, PACKAGE_FILE_NAME), Collections.singletonList("Test content"));
		FileUtils.copyFile(TestFiles.find(PACKAGE_ARCHIVE), archive);
		p.archive = archive;
	}

	private OnDiskPackageRepository makeRepository() {
		OnDiskPackageRepository instance = new OnDiskPackageRepository(repositoryDir, taskRepository, licence);
		instance.setPackageInstallationRepository(packageInstallationRepository);
		return instance;
	}

	@Test
	public void storesPackagesInRepositoryFolder() {
		toWrite.insert(p);

		assertTrue(p.path.toPath().startsWith(repositoryDir.toPath()));
		assertTrue(new File(p.path, PACKAGE_FILE_NAME).exists());
		File expectedArchiveFile = new File(toWrite.getDirectory(p), PACKAGE_ARCHIVE);
		assertTrue(expectedArchiveFile.exists());
		assertEquals(expectedArchiveFile, p.archive);

		assertThat(archive.exists(), is(false));
	}
	
	@Test
	public void removesPackagesFromRepository() {
		toWrite.insert(p);
		toWrite.delete(p.id);
		
		assertEquals(0, toWrite.listAll().size());
		assertEquals(0, repositoryDir.list().length);
		assertFalse(p.path.exists());
	}
	
	@Test
	public void writeAndRead() {
		toWrite.insert(p);
		
		toRead.loadFromDisk();
		
		assertEquals(1, toRead.listAll().size());
		Package loaded = toRead.listAll().get(0);
		assertEquals(loaded.id, p.id);
		assertEquals(PackageType.DataSupply, loaded.type);
		assertEquals(loaded.size, p.size);
		assertEquals(1, (int)loaded.slot);
		assertEquals(loaded.version, p.version);
		assertEquals(loaded.source, p.source);
		assertTrue(new File(loaded.path, PACKAGE_FILE_NAME).exists());
		File expectedArchiveFile = new File(toRead.getDirectory(loaded), PACKAGE_ARCHIVE);
		assertEquals(expectedArchiveFile, loaded.archive);
	}
	
	@Test
	public void listsPackagesByType() {
		toWrite.insert(p);
		
		toRead.loadFromDisk();
		
		assertEquals(0, toRead.listByType(PackageType.CopyStick).size());
		assertEquals(1, toRead.listByType(PackageType.DataSupply).size());
		assertEquals(p.id, toRead.listByType(PackageType.DataSupply).get(0).id);
	}
	
	@Test
	public void loadsLegacyXmlFile() throws Exception {
		FileUtils.copyFileToDirectory(TestFiles.find("legacy-database-files/package.xml"), repositoryDir.append("2e4551dd-3a09-44b4-965f-070bf28d8816"));
		
		toRead.loadFromDisk();
		
		Package p = toRead.tryFindById(UUID.fromString("2e4551dd-3a09-44b4-965f-070bf28d8816"));
		assertEquals(PackageType.Indis5MultimediaContent, p.type);
		assertEquals("User: Unknown", p.source);
	}

	@Test(expected = PackageTypeNotLicenced.class)
	public void insertUnlicesedPackageType() {
		when(licence.isPackageTypeAvailable(any())).thenReturn(false);
		toWrite.insert(p);
	}

	@Test
	public void insertsDuplicateToRepository() throws IOException {
		toWrite.insert(p);

		Package copy = toWrite.duplicate(p, null);

		assertEquals(2, toWrite.listAll().size());
		assertNotEquals(p.id, copy.id);
		assertEquals(p.type, copy.type);
		assertEquals(p.version, copy.version);
		assertNotEquals(p.path, copy.path);
		assertEquals(p.size, copy.size);
		assertEquals(p.slot, copy.slot);
		assertEquals(p.startOfPeriod, copy.startOfPeriod);
		assertEquals(p.endOfPeriod, copy.endOfPeriod);

		assertEquals(FileUtils.sizeOf(p.path), FileUtils.sizeOf(copy.path));
		File expectedArchiveFile = new File(toWrite.getDirectory(copy), PACKAGE_ARCHIVE);
		assertTrue(expectedArchiveFile.exists());
		assertEquals(expectedArchiveFile, copy.archive);

		File originalArchiveFile = new File(toWrite.getDirectory(p), PACKAGE_ARCHIVE);
		assertTrue(originalArchiveFile.exists());
	}

	@Test
	public void writeAndRead_withRunningInstallation() {
		Task t = new Task(p, v, null);
		p.installation = new PackageInstallation(UUID.randomUUID(), Collections.singletonList(t));
		toWrite.insert(p);

		verify(packageInstallationRepository).insertOrReplace(p.installation);

		OnDiskPackageRepository another = makeRepository();
		when(packageInstallationRepository.tryFindById(p.installation.id())).thenReturn(p.installation);
		another.loadFromDisk();
		Package read = another.tryFindById(p.id);

		assertNotNull(read);
		assertThat(read.id, is(p.id));
		verify(packageInstallationRepository).tryFindById(p.installation.id());
		assertNotNull(read.installation);
		assertThat(read.installation, sameInstance(p.installation));
	}

	@Test
	public void noException_WhenXmlFileIsEmpty() throws Exception {
		TempFile folder = repositoryDir.newFolder(UUID.randomUUID().toString());
		String xmlFile = toWrite.getXmlFile(folder).file().getName();
        folder.newFile(xmlFile);

		toWrite.loadFromDisk();
	}

	@Test
	public void completesInstallation_oneTask() {
		toWrite.insert(p);

		Task t1 = new Task(p, v, null);
		p.startInstallation(Arrays.asList(t1));
		assertNotNull(p.installation);
		t1.setServerStatus(TaskStatus.ServerStatus.Finished);

		toWrite.onApplicationEvent(new TaskCompleteEvent(this, t1));

		Package loaded = toWrite.tryFindById(p.id);
		assertNull(loaded.installation);
		verify(packageInstallationRepository).insertOrReplace(p.installation);
		assertThat(p.installation.getEndDatetime(), is(notNullValue()));

		toRead.loadFromDisk();
		loaded = toRead.tryFindById(p.id);
		assertNull(loaded.installation);
	}

	@Test
	public void completesInstallation_twoTasks() {
		toWrite.insert(p);

		Vehicle v2 = createVehicle("vehicle2");
		Task t1 = new Task(p, v, null);
		Task t2 = new Task(p, v2, null);
		p.startInstallation(Arrays.asList(t1, t2));
		when(taskRepository.tryFindById(t1.getId())).thenReturn(t1);
		when(taskRepository.tryFindById(t2.getId())).thenReturn(t2);

		t1.setServerStatus(TaskStatus.ServerStatus.Finished);
		toWrite.onApplicationEvent(new TaskCompleteEvent(this, t1));

		Package loaded = toWrite.tryFindById(p.id);
		assertNotNull(loaded.installation);
		verifyZeroInteractions(packageInstallationRepository);
		assertThat(p.installation.getEndDatetime(), nullValue());

		t2.setServerStatus(TaskStatus.ServerStatus.Finished);
		toWrite.onApplicationEvent(new TaskCompleteEvent(this, t2));

		loaded = toWrite.tryFindById(p.id);
		assertNull(loaded.installation);
		verify(packageInstallationRepository).insertOrReplace(p.installation);
		assertThat(p.installation.getEndDatetime(), is(notNullValue()));
	}
}
