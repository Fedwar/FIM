package fleetmanagement.frontend.controllers;

import com.sun.jersey.core.header.FormDataContentDisposition;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.preprocess.Preprocessor;
import fleetmanagement.backend.repositories.exception.PackageTypeNotLicenced;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus.ServerStatus;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.TempDirectory;
import fleetmanagement.frontend.model.PackageDetails;
import fleetmanagement.frontend.model.PackagesAndGroups;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import fleetmanagement.usecases.DeletePackage;
import fleetmanagement.usecases.ImportPackage;
import fleetmanagement.usecases.InstallPackage;
import gsp.configuration.LocalFiles;
import gsp.testutil.TemporaryDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static fleetmanagement.frontend.controllers.Packages.INSTALLATION_MESSAGE_PARAM;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackagesTest {

    private InstallPackage installPackage;
    private ImportPackage importPackage;
    private DeletePackage deletePackage;
    private TestScenarioPrefilled scenario;
    private Packages tested;
    private Package p;
    private TemporaryDirectory dataDir;
    private SessionStub request;
    @Mock
    private Preprocessor preprocessor;

    @BeforeClass
    public static void setupClass() {
        TestScenarioPrefilled scenario = new TestScenarioPrefilled();
        Templates.init(null, null);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        dataDir = TemporaryDirectory.create();
        scenario = new TestScenarioPrefilled();
        p = scenario.package1;
        installPackage = new InstallPackage(scenario.taskRepository, scenario.packageRepository, scenario.vehicleRepository, scenario.licence);
        importPackage = new ImportPackage(scenario.packageRepository, new TempDirectory(dataDir), scenario.licence, preprocessor);
        deletePackage = new DeletePackage(scenario.packageRepository, scenario.taskRepository, scenario.vehicleRepository, scenario.groupRepository);
        tested = new Packages(new SessionStub(), scenario.packageRepository, scenario.vehicleRepository, scenario.groupRepository, scenario.taskRepository, installPackage, importPackage, deletePackage, scenario.licence);
        request = new SessionStub();
    }

    @After
    public void teardown() {
        dataDir.delete();
    }

    @Test
    public void servesPackageList() {
        ModelAndView<PackagesAndGroups> served = tested.getListPackagesUI();
        assertEquals("package-list.html", served.page);
        assertEquals(2, served.viewmodel.packages.categories.size());
        assertEquals("Data Supply 1.0 (Slot 1)", served.viewmodel.packages.categories.get(0).packages.get(0).name);
        assertEquals("Data Supply 1.1 (Slot 2)", served.viewmodel.packages.categories.get(0).packages.get(1).name);
    }

    @Test
    public void importsUploadedDatasupply() throws Exception {
        tested.upload(new FileInputStream(LocalFiles.find("Nearly_Empty_DV.zip")), new FormDataContentDisposition("form-data; name=\"foo\"; filename=\"Nearly_Empty_DV.zip\""), null);
        assertNearlyEmptyDvIsImported();
    }

    @Test
    public void importsUploadedCopystick() throws Exception {
        tested.upload(new FileInputStream(LocalFiles.find("RemoteCopyStick_DoSomething.zip")), new FormDataContentDisposition("form-data; name=\"foo\"; filename=\"RemoteCopyStick_DoSomething.zip\""), null);
        assertCopystickIsInstalled();
    }

    @Test
    public void reportsErrorsDuringImports() throws Exception {
        Response r = tested.upload(new FileInputStream(LocalFiles.find("test.zip")), new FormDataContentDisposition("form-data; name=\"foo\"; filename=\"test.zip\""), null);

        assertEquals(500, r.getStatus());
        assertEquals("application/json", r.getMetadata().getFirst("Content-Type").toString());
    }

    @Test
    public void isCompatibleToBrowsersNotSupportingContentDisposition() throws Exception {
        tested.upload(new FileInputStream(LocalFiles.find("RemoteCopyStick_DoSomething.zip")), null, "RemoteCopyStick_DoSomething.zip");
        assertCopystickIsInstalled();
    }

    @Test
    public void removesPathFromImportedFilename() throws Exception {
        tested.upload(new FileInputStream(LocalFiles.find("RemoteCopyStick_DoSomething.zip")), null, "foo\\bar/RemoteCopyStick_DoSomething.zip");
        assertCopystickIsInstalled();
    }

    @Test
    public void allowsDeletingPackage() {
        tested.deletePackage(p.id.toString());
        assertEquals(4, scenario.packageRepository.listAll().size());
    }

    @Test
    public void servesPackageDetails() throws UnsupportedEncodingException {
        ModelAndView<PackageDetails> served = tested.getPackageDetailsUI(p.id.toString(), null, null);
        assertTrue(served.viewmodel.name.contains("Data Supply 1.0"));
        assertTrue(served.viewmodel.version.contains(p.version));
    }

    @Test
    public void packageDetails_showsSuccess() throws UnsupportedEncodingException {
        final int taskCount = 2;
        ModelAndView<PackageDetails> served = tested.getPackageDetailsUI(p.id.toString(), taskCount, null);
        assertThat(served.viewmodel.message, notNullValue());
        assertThat(served.viewmodel.message.text, is(I18n.get(request, "installing_on_x_vehicles", taskCount)));
    }

    @Test
    public void packageDetails_showsFailure() throws UnsupportedEncodingException {
        ModelAndView<PackageDetails> served = tested.getPackageDetailsUI(p.id.toString(), 0, null);
        assertThat(served.viewmodel.message, notNullValue());
        assertThat(served.viewmodel.message.text, is(I18n.get(request, "select_vehicle_for_installation")));
    }

    @Test
    public void allowsStartingPackageInstallation() throws Exception {
        tested.startPackageInstallation(p.id.toString(), Arrays.asList(scenario.vehicle1.id.toString(), scenario.vehicle2.id.toString()));
        assertEquals(2, scenario.taskRepository.getTasksByPackage(p.id).size());
    }

    @Test
    public void proposesCancellingConflictingTasksOnPackageInstallationDVDifferentSlots() throws Exception {
        tested.startPackageInstallation(p.id.toString(), Arrays.asList(scenario.vehicle1.id.toString(), scenario.vehicle2.id.toString()));
        Package pkg = scenario.packageRepository.tryFindById(p.id);
        assertThat(pkg.installation, notNullValue());
        assertThat(pkg.installation.getConflictingTasks(), empty());

        ModelAndView<PackageDetails> served = tested.getPackageDetailsUI(pkg.id.toString(), null, null);
        assertThat(served.viewmodel.conflictingTasksModal, nullValue());

        tested.startPackageInstallation(scenario.package2.id.toString(), Arrays.asList(scenario.vehicle1.id.toString(), scenario.vehicle2.id.toString()));
        pkg = scenario.packageRepository.tryFindById(scenario.package2.id);
        assertThat(pkg.installation, notNullValue());
        assertThat(pkg.installation.getConflictingTasks(), empty());
    }

    @Test
    public void proposesCancellingConflictingTasksOnPackageInstallationDVSameSlot() throws Exception {
        tested.startPackageInstallation(p.id.toString(), Arrays.asList(scenario.vehicle1.id.toString(), scenario.vehicle2.id.toString()));
        Package pkg = scenario.packageRepository.tryFindById(p.id);
        assertThat(pkg.installation, notNullValue());
        assertThat(pkg.installation.getConflictingTasks(), empty());

        tested.startPackageInstallation(scenario.package3.id.toString(), Arrays.asList(scenario.vehicle1.id.toString(), scenario.vehicle2.id.toString()));
        pkg = scenario.packageRepository.tryFindById(scenario.package3.id);
        assertThat(pkg.installation, notNullValue());
        assertThat(pkg.installation.getConflictingTasks(), not(empty()));

        ModelAndView<PackageDetails> served = tested.getPackageDetailsUI(pkg.id.toString(), 0, null);
        assertThat(served.viewmodel.conflictingTasksModal, notNullValue());
    }

    @Test
    public void proposesCancellingConflictingTasksOnPackageInstallation() throws Exception {
        tested.startPackageInstallation(scenario.package4.id.toString(), Arrays.asList(scenario.vehicle1.id.toString(), scenario.vehicle2.id.toString()));
        Package pkg = scenario.packageRepository.tryFindById(scenario.package4.id);
        assertThat(pkg.installation, notNullValue());
        assertThat(pkg.installation.getConflictingTasks(), empty());

        tested.startPackageInstallation(scenario.package5.id.toString(), Arrays.asList(scenario.vehicle1.id.toString(), scenario.vehicle2.id.toString()));
        pkg = scenario.packageRepository.tryFindById(scenario.package5.id);
        assertThat(pkg.installation, notNullValue());
        assertThat(pkg.installation.getConflictingTasks(), not(empty()));

        ModelAndView<PackageDetails> served = tested.getPackageDetailsUI(pkg.id.toString(), 0, null);
        assertThat(served.viewmodel.conflictingTasksModal, notNullValue());
    }

    @Test
    public void allowsCancellingTasks() throws Exception {
        tested.startPackageInstallation(p.id.toString(), Arrays.asList(scenario.vehicle1.id.toString(), scenario.vehicle2.id.toString()));
        List<Task> tasks = scenario.taskRepository.getTasksByPackage(p.id);
        tested.cancelConflictingTasks(p.id.toString(), Arrays.asList(tasks.get(0).getId().toString(), tasks.get(1).getId().toString()));
        assertTrue(scenario.taskRepository.getTasksByPackage(p.id).stream().allMatch(t -> t.getStatus().serverStatus == ServerStatus.Cancelled));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void showsErrorWhenImportUnlicencedPackageType() throws Exception {
        scenario.licence.availablePackageTypes = new HashSet<>();

        importPackage = mock(ImportPackage.class);
        when(importPackage.importPackage(anyString(), any(), anyString(), anyString())).thenThrow(new PackageTypeNotLicenced(null));

        tested = new Packages(new SessionStub(), scenario.packageRepository, scenario.vehicleRepository, scenario.groupRepository, scenario.taskRepository, installPackage, importPackage, deletePackage, scenario.licence);

        Response r = tested.upload(new FileInputStream(LocalFiles.find("RemoteCopyStick_DoSomething.zip")), new FormDataContentDisposition("form-data; name=\"foo\"; filename=\"RemoteCopyStick_DoSomething.zip\""), null);
        String errorMessage = ((Packages.Error) r.getEntity()).errorMessage;
        assertEquals(errorMessage, I18n.get(request, "licence_package_type_disabled"));
    }

    @Test
    public void showsErrorWhenInstallUnlicencedPackageType() throws Exception {
        scenario.licence.availablePackageTypes = new HashSet<>();

        Response resp = tested.startPackageInstallation(p.id.toString(), Arrays.asList(scenario.vehicle1.id.toString(), scenario.vehicle2.id.toString()));

        String message = I18n.get(request, "licence_package_type_disabled");
        String encoded = URLEncoder.encode(message, "UTF-8");
        URI location = (URI) resp.getMetadata().getFirst(HttpHeaders.LOCATION);
        assertThat(location.getQuery(), is(INSTALLATION_MESSAGE_PARAM + "=" + encoded));

        ModelAndView<PackageDetails> served = tested.getPackageDetailsUI(p.id.toString(), null, encoded);
        assertThat(served.viewmodel.message, notNullValue());
        assertThat(served.viewmodel.message.text, is(message));
    }


    private void assertNearlyEmptyDvIsImported() {
        assertEquals(6, scenario.packageRepository.listAll().size());
        Package imported = scenario.packageRepository.listAll().get(5);
        assertEquals(PackageType.DataSupply, imported.type);
        assertEquals("V2015.04", imported.version);
        assertEquals(1, (int) imported.slot);
    }

    private void assertCopystickIsInstalled() {
        assertEquals(6, scenario.packageRepository.listAll().size());
        Package imported = scenario.packageRepository.listAll().get(4);
        assertEquals(PackageType.CopyStick, imported.type);
        assertEquals("2.0", imported.version);
    }

}
