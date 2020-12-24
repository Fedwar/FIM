package fleetmanagement.frontend.controllers;

import com.sun.jersey.api.NotFoundException;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.tasks.TaskStatus.ServerStatus;
import fleetmanagement.backend.vehicles.LiveInformation;
import fleetmanagement.backend.vehicles.LiveInformation.Position;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.frontend.model.DiagnosisDetails;
import fleetmanagement.frontend.model.TaskDetails;
import fleetmanagement.frontend.model.VehicleDetails;
import fleetmanagement.frontend.model.VehicleList;
import fleetmanagement.frontend.model.VehicleTaskList;
import fleetmanagement.frontend.model.VehiclesAndGroups;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import fleetmanagement.usecases.DeleteVehicle;
import fleetmanagement.usecases.InstallPackage;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VehiclesTest {

    private TestScenarioPrefilled scenario;
    private Vehicles tested;
    private Vehicle v;
    private Package p;

    @Before
    public void setup() {
        scenario = new TestScenarioPrefilled();
        v = scenario.vehicle1;
        p = scenario.package1;

        InstallPackage installPackage = new InstallPackage(scenario.taskRepository, scenario.packageRepository, scenario.vehicleRepository, scenario.licence);
        DeleteVehicle deleteVehicle = new DeleteVehicle(scenario.vehicleRepository, scenario.taskRepository, scenario.diagnosisRepository);
        tested = new Vehicles(new SessionStub(), scenario.vehicleRepository, scenario.groupRepository
                , scenario.taskRepository, scenario.packageRepository, scenario.diagnosisRepository
                , installPackage, deleteVehicle, scenario.licence);
    }

    @Test
    public void servesVehicleList() {
        ModelAndView<VehiclesAndGroups> served = tested.list();
        List<VehicleList.Vehicle> vehicles = served.viewmodel.vehicleList.getVehicles();
        assertEquals(scenario.vehicle1.getName(), vehicles.get(0).name);
        assertEquals(scenario.vehicle2.getName(), vehicles.get(1).name);
    }

    @Test
    public void servesVehicleDetails() {
        ModelAndView<VehicleDetails> served = tested.vehicleDetail(v.id.toString());

        assertEquals(v.getName(), served.viewmodel.name);
        assertEquals(v.uic, served.viewmodel.uic);
    }

    @Test
    public void allowsStartingAndCancellingPackageInstallation() {
        Response servedResponse = tested.startPackageInstallation(v.id.toString(), p.id.toString());
        assertIsRedirectionTo(servedResponse, "/vehicles/" + v.id.toString() + "/installation-started/" + p.id.toString());

        assertEquals(1, v.getTaskIds().size());
        Task task = v.getRunningTasks(scenario.taskRepository).get(0);
        assertEquals(p, task.getPackage());

        tested.cancelTask(v.id.toString(), task.getId().toString());

        assertEquals(0, v.getRunningTasks(scenario.taskRepository).size());
    }

    @Test
    public void servesVehicleDetailsWithStatusMessage() {
        String vehicleId = v.id.toString();
        String packageId = p.id.toString();
        tested.startPackageInstallation(vehicleId, packageId);

        ModelAndView<VehicleDetails> served = tested.showPackageInstallationSuccess(vehicleId, packageId);
        assertEquals("Started installation of Data Supply 1.0 (Slot 1).", served.viewmodel.message.text);

        served = tested.showPackageInstallationError(vehicleId);
        assertEquals("No installation was started! Please select a package.", served.viewmodel.message.text);

        UUID taskId = v.getTaskIds().get(0);
        served = tested.showTaskCancelSuccess(vehicleId, taskId.toString());
        assertEquals("Cancelled installation of \"Data Supply 1.0 (Slot 1)\".", served.viewmodel.message.text);
    }

    @Test
    public void servesRunningTasksSnippetForVehicleDetailAjaxUpdate() {
        scenario.addTask(v, p);
        ModelAndView<VehicleDetails> served = tested.taskSnippet(v.id.toString());
        assertEquals("Data Supply 1.0 (Slot 1)", served.viewmodel.runningTasks.get(0).packageName);
    }

    @Test
    public void servesListOfAllTasksForGivenVehicle() {
        scenario.addTask(v, p);
        Task t2 = scenario.addTask(v, scenario.package2);
        t2.cancel();

        ModelAndView<VehicleTaskList> served = tested.allTasksList(v.id.toString());

        List<fleetmanagement.frontend.model.VehicleTaskList.Task> tasks = new ArrayList<>();
        served.viewmodel.iterator().forEachRemaining(tasks::add);
        assertEquals(2, tasks.size());
        assertTrue(tasks.stream().anyMatch(t -> t.packageName.equals("Data Supply 1.0 (Slot 1)")));
        assertTrue(tasks.stream().anyMatch(t -> t.packageName.equals("Data Supply 1.1 (Slot 2)")));
    }

    @Test
    public void setsStatusTextInAllTasksList() {
        Task t = scenario.addTask(v, p);

        assertStatusTextEquals("Pending, 0%", ServerStatus.Pending, ClientStage.PENDING, t);
        assertStatusTextEquals("Initiated, waiting for start of the transfer, 0%", ServerStatus.Pending, ClientStage.INITIALIZING, t);
        assertStatusTextEquals("Transferring files, 0%", ServerStatus.Running, ClientStage.DOWNLOADING, t);
        assertStatusTextEquals("Cancelled, 0%", ServerStatus.Cancelled, ClientStage.INITIALIZING, t);
        assertStatusTextEquals("Finished, 0%", ServerStatus.Finished, ClientStage.FINISHED, t);
        assertStatusTextEquals("Failed, 0%", ServerStatus.Failed, ClientStage.CANCELLED, t);
        assertStatusTextEquals("Failed, 0%", ServerStatus.Failed, ClientStage.FAILED, t);
    }

    private void assertStatusTextEquals(String expectedText, ServerStatus forStatus, ClientStage stage, Task t) {
        t.setServerStatus(forStatus);
        t.setClientStatus(stage, 0);
        ModelAndView<VehicleTaskList> served = tested.allTasksList(v.id.toString());
        assertEquals(expectedText, served.viewmodel.iterator().next().status);
    }

    @Test
    public void setsStatusIconInTasklist() {
        Task t = scenario.addTask(v, p);

        assertStatusIconCssEquals("Running", ServerStatus.Pending, t);
        assertStatusIconCssEquals("Running", ServerStatus.Running, t);
        assertStatusIconCssEquals("Cancelled", ServerStatus.Cancelled, t);
        assertStatusIconCssEquals("Finished", ServerStatus.Finished, t);
        assertStatusIconCssEquals("Failed", ServerStatus.Failed, t);
    }

    private void assertStatusIconCssEquals(String expectedCssClass, ServerStatus forStatus, Task t) {
        t.setServerStatus(forStatus);
        ModelAndView<VehicleTaskList> served = tested.allTasksList(v.id.toString());
        assertEquals(expectedCssClass, served.viewmodel.iterator().next().statusCssClass);
    }

    @Test
    public void servesTaskDetails() {
        Task t = scenario.addTask(v, p);
        t.cancel();

        ModelAndView<TaskDetails> served = tested.taskDetail(v.id.toString(), t.getId().toString());

        assertEquals("Data Supply 1.0 (Slot 1)", served.viewmodel.packageDescription);
        assertTrue(served.viewmodel.logs.stream().anyMatch(x -> x.message.contains("created")));
        assertTrue(served.viewmodel.logs.stream().anyMatch(x -> x.message.contains("cancelled")));
        assertTrue(served.viewmodel.logs.stream().anyMatch(x -> x.message.contains(t.getId().toString())));
    }

    @Test
    public void allowsTaskLogDownload() {
        Task t = scenario.addTask(v, p);
        t.cancel();

        Response response = tested.downloadTaskLog(t.getId().toString());

        assertTrue(response.getEntity().toString().contains("Task created with id " + t.getId()));
        assertEquals("attachment; filename=task-" + t.getId() + "-log.txt", getHeader(response, "content-disposition"));
    }

    private String getHeader(Response response, String header) {
        return response.getMetadata().get(header).get(0).toString();
    }

    @Test
    public void servesPackageInstallationDialog() {
        ModelAndView<VehicleDetails> served = tested.showInstallPackageModal(v.id.toString());

        assertNotNull(served.viewmodel.installPackageModal);
        assertEquals("Data Supply", served.viewmodel.installPackageModal.packageTypes.get(0).name);
        assertEquals("Data Supply 1.0 (Slot 1)", served.viewmodel.installPackageModal.packageTypes.get(0).installablePackages.get(0).name);
        assertEquals("Data Supply 1.1 (Slot 2)", served.viewmodel.installPackageModal.packageTypes.get(0).installablePackages.get(1).name);
    }

    @Test
    public void showsMapLinkOnlyIfSomeVehicleLocationsAreKnown() {
        ModelAndView<VehiclesAndGroups> served = tested.list();
        assertFalse(served.viewmodel.vehicleList.showMapLink);

        v.liveInformation = new LiveInformation(new Position(52, 13), null, null, null, null, Collections.emptyList(), null);
        served = tested.list();
        assertTrue(served.viewmodel.vehicleList.showMapLink);
    }

    @Test
    public void deletesVehicle() {
        tested.startPackageInstallation(v.id.toString(), p.id.toString());
        Task task = v.getRunningTasks(scenario.taskRepository).get(0);
        Response servedResponse = tested.deleteVehicle(v.id.toString());
        assertIsRedirectionTo(servedResponse, "/vehicles");

        assertFalse(scenario.vehicleRepository.listAll().contains(v));
        assertNull(scenario.taskRepository.tryFindById(task.getId()));
    }

    @Test(expected = NotFoundException.class)
    public void shows404WhenAccessingDiagnosisForUnknownVehicle() {
        tested.showDiagnosisDetails("00000000-0000-0000-0000-000000000000");
    }

    @Test
    public void showDiagnosisIfAvailable() {
        ModelAndView<Object> served = tested.showDiagnosisDetails(scenario.vehicle1.id.toString());

        DiagnosisDetails vm = (DiagnosisDetails) served.viewmodel;
        assertEquals("diagnosis-details.html", served.page);
        assertEquals(scenario.vehicle1.id.toString(), vm.vehicleId);
    }

    @Test
    public void diagnosisDetailsAjax() {
        ModelAndView<Object> served = tested.diagnosisDetailsAjax(scenario.vehicle1.id.toString());

        DiagnosisDetails vm = (DiagnosisDetails) served.viewmodel;
        assertEquals("diagnosis-details-template.html", served.page);
        assertEquals(scenario.vehicle1.id.toString(), vm.vehicleId);
    }


    @Test
    public void showsGeoIfLicenced() {
        scenario.licence.vehicleGeo = false;
        ModelAndView<VehicleDetails> served = tested.vehicleDetail(scenario.vehicle1.id.toString());
        assertFalse(served.viewmodel.showGeo);

        scenario.licence.vehicleGeo = true;
        served = tested.vehicleDetail(scenario.vehicle1.id.toString());
        assertTrue(served.viewmodel.showGeo);
    }


    private void assertIsRedirectionTo(Response servedResponse, String redirectedTo) {
        assertEquals(Status.SEE_OTHER.getStatusCode(), servedResponse.getStatus());
        System.out.println(servedResponse.getMetadata().get("Location"));
        assertEquals(redirectedTo, servedResponse.getMetadata().get("Location").get(0).toString());
    }

    @Test
    public void filteredList() {
        v.setGroupId(scenario.group1.id.toString());
        ModelAndView<VehiclesAndGroups> model = tested.filteredList(scenario.group1.id.toString());

        assertEquals(1, model.viewmodel.vehicleList.getVehicles().size());
        VehicleList.Vehicle modelVehicle = model.viewmodel.vehicleList.getVehicles().get(0);
        assertEquals(v.getName(), modelVehicle.name);
        assertEquals(scenario.group1.name, modelVehicle.groupName);
    }


    @Test
    public void editName() {
        String newName = "newName";
        tested.editName(v.id.toString(), newName);
        assertEquals(v.getName(), newName);
        tested.editName(v.id.toString(), "Vehicle 1");
    }
}
