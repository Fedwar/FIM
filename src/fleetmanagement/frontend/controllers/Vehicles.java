package fleetmanagement.frontend.controllers;

import com.sun.jersey.api.NotFoundException;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.ActivityLog;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskLogFile;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.DiagnosedDeviceModel;
import fleetmanagement.frontend.model.DiagnosisDetails;
import fleetmanagement.frontend.model.GroupList;
import fleetmanagement.frontend.model.InstallPackageModal;
import fleetmanagement.frontend.model.Name;
import fleetmanagement.frontend.model.StatusMessage;
import fleetmanagement.frontend.model.StatusMessage.ErrorMessage;
import fleetmanagement.frontend.model.StatusMessage.SuccessMessage;
import fleetmanagement.frontend.model.TaskDetails;
import fleetmanagement.frontend.model.VehicleDetails;
import fleetmanagement.frontend.model.VehicleList;
import fleetmanagement.frontend.model.VehicleTaskList;
import fleetmanagement.frontend.model.VehiclesAndGroups;
import fleetmanagement.frontend.security.webserver.UserRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.usecases.DeleteVehicle;
import fleetmanagement.usecases.InstallPackage;
import fleetmanagement.usecases.InstallPackage.StartInstallationResult;
import fleetmanagement.usecases.ListVehicleDetails;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;

@Path("vehicles")
@Component
public class Vehicles extends FrontendController {
    private final VehicleRepository vehicles;
    private final GroupRepository groups;
    private final TaskRepository tasks;
    private final PackageRepository packages;
    private final DiagnosisRepository diagnoses;
    private final InstallPackage installPackage;
    private final DeleteVehicle deleteVehicle;
    private final Licence licence;

    private static final Logger logger = Logger.getLogger(Vehicles.class);

    @Autowired
    public Vehicles(
            UserSession session, VehicleRepository vehicles, GroupRepository groups, TaskRepository tasks,
            PackageRepository packages, DiagnosisRepository diagnoses,
            InstallPackage installPackage, DeleteVehicle deleteVehicle, Licence licence) {
        super(session);
        this.vehicles = vehicles;
        this.groups = groups;
        this.tasks = tasks;
        this.packages = packages;
        this.diagnoses = diagnoses;
        this.installPackage = installPackage;
        this.deleteVehicle = deleteVehicle;
        this.licence = licence;
    }

    @GET
    public ModelAndView<VehiclesAndGroups> list() {
        return new ModelAndView<>("vehicle-list.html",
                new VehiclesAndGroups(
                        new VehicleList(vehicles.listAll(), groups.mapAll(), tasks, session, licence, null),
                        new GroupList(groups.listAll(), null, vehicles)));
    }

    @GET
    @Path("group/{groupId}")
    public ModelAndView<VehiclesAndGroups> filteredList(@PathParam("groupId") String groupId) {
        if (groupId == null)
            throw new IllegalArgumentException("Invalid argument of group/{groupId}. groupId cannot be null.");
        return new ModelAndView<>("vehicle-list.html",
                new VehiclesAndGroups(
                        new VehicleList(vehicles.listAll(), groups.mapAll(), tasks, session, licence, groupId),
                        new GroupList(groups.listAll(), groupId, vehicles)));
    }

    @GET
    @Path("{id}")
    public ModelAndView<VehicleDetails> vehicleDetail(@PathParam("id") String id) {
        return showVehicleDetailsWithOptionalStatusMessageAndModal(id, null, null);
    }

    @GET
    @Path("{id}/cancel-all")
    public ModelAndView<VehicleDetails> cancelAllRunningTasks(@PathParam("id") String id) {
        Vehicle vehicle = vehicles.tryFindById(UUID.fromString(id));

        if (vehicle == null) {
            logger.error("Vehicle doesn't exist! " + id);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        for (Task task : vehicle.getRunningTasks(tasks)) {
            ActivityLog.vehicleMessage(ActivityLog.Operations.INSTALLATION_CANCELLED, task.getPackage(),
                    vehicles.tryFindById(task.getVehicleId()), session.getSecurityContext().getUserPrincipal().getName());

            tasks.update(UUID.fromString(task.getId().toString()), toCancel -> {
                if (toCancel == null)
                    throw new WebApplicationException(Status.NOT_FOUND);

                toCancel.cancel();
            });
        }

        return showVehicleDetailsWithOptionalStatusMessageAndModal(
                id,
                new SuccessMessage((i18n(
                        "cancelled_installation_of",
                        i18n("all_packages_of") + " \"" + vehicle.getName() + "\""
                ))),
                null
        );
    }

    @POST
    @Path("{id}/tasks/{taskId}/cancel")
    @UserRoleRequired
    public Response cancelTask(@PathParam("id") String id, @PathParam("taskId") String taskId) {
        Task task = tasks.tryFindById(UUID.fromString(taskId));
        if (task == null)
            throw new NotFoundException("Task doesn't exist! " + taskId);

        ActivityLog.vehicleMessage(ActivityLog.Operations.INSTALLATION_CANCELLED, task.getPackage(),
                vehicles.tryFindById(task.getVehicleId()), session.getSecurityContext().getUserPrincipal().getName());

        tasks.update(UUID.fromString(taskId), toCancel -> {
            if (toCancel == null)
                throw new WebApplicationException(Status.NOT_FOUND);

            toCancel.cancel();
        });

        return Redirect.to("/vehicles/" + id + "/task-cancelled/" + taskId);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}/ajax/tasks")
    public ModelAndView<VehicleDetails> taskSnippet(@PathParam("id") String id) {
        fleetmanagement.backend.vehicles.Vehicle vehicle = findVehicle(id);
        return new ModelAndView<>("vehicle-details-tasks.html",
                new ListVehicleDetails(groups, packages, tasks, licence).listVehicleDetails(vehicle, session));
    }

    Vehicle findVehicle(String id) {
        fleetmanagement.backend.vehicles.Vehicle vehicle = vehicles.tryFindById(UUID.fromString(id));
        if (vehicle == null) {
            logger.error("Vehicle doesn't exist! " + id);
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return vehicle;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}/ajax/data")
    public ModelAndView<VehicleDetails> vehicleDetailsData(@PathParam("id") String id) {
        fleetmanagement.backend.vehicles.Vehicle vehicle = findVehicle(id);
        return new ModelAndView<>("vehicle-details-data-template.html",
                new ListVehicleDetails(groups, packages, tasks, licence).listVehicleDetails(vehicle, session));
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}/ajax/info")
    public ModelAndView<VehicleDetails> vehicleDetailsInfo(@PathParam("id") String id) {
        fleetmanagement.backend.vehicles.Vehicle vehicle = findVehicle(id);
        return new ModelAndView<>("vehicle-details-info-template.html",
                new ListVehicleDetails(groups, packages, tasks, licence).listVehicleDetails(vehicle, session));
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Path("{id}/diagnosis/ajax")
    public ModelAndView<Object> diagnosisDetailsAjax(@PathParam("id") String id) {
        UUID vehicleId = UUID.fromString(id);
        Vehicle vehicle = vehicles.tryFindById(vehicleId);

        if (vehicle == null)
            throw new NotFoundException("Vehicle doesn't exist! " + id);

        Diagnosis diagnosis = null;
        if (licence.isDiagnosisInfoAvailable()) {
            diagnosis = diagnoses.tryFindByVehicleId(vehicleId);
        }

        return new ModelAndView<>("diagnosis-details-template.html", new DiagnosisDetails(diagnosis, vehicle, licence, session));
    }


    @GET
    @Path("ajax/list")
    public ModelAndView<VehicleList> vehicleListAjax() {
        return new ModelAndView<>("vehicle-list-template.html",
                new VehicleList(vehicles.listAll(), groups.mapAll(), tasks, session, licence, null));
    }

    @GET
    @Path("ajax/group/{groupId}")
    public ModelAndView<VehicleList> vehicleGroupAjax(@PathParam("groupId") String groupId) {
        return new ModelAndView<>("vehicle-list-template.html",
                        new VehicleList(vehicles.listAll(), groups.mapAll(), tasks, session, licence, groupId));
    }

    @GET
    @Path("{id}/tasks")
    public ModelAndView<VehicleTaskList> allTasksList(@PathParam("id") String id) {
        fleetmanagement.backend.vehicles.Vehicle vehicle = vehicles.tryFindById(UUID.fromString(id));
        if (vehicle == null) {
            logger.error("Vehicle doesn't exist! " + id);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return new ModelAndView<>("vehicle-alltaskslist.html", new VehicleTaskList(vehicle, tasks, session));
    }

    @GET
    @Path("{id}/tasks/{taskId}")
    public ModelAndView<TaskDetails> taskDetail(@PathParam("id") String id, @PathParam("taskId") String taskId) {
        fleetmanagement.backend.vehicles.Vehicle vehicle = vehicles.tryFindById(UUID.fromString(id));
        fleetmanagement.backend.tasks.Task task = tasks.tryFindById(UUID.fromString(taskId));
        if (vehicle == null) {
            logger.error("Vehicle doesn't exist! " + id);
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        if (task == null) {
            logger.error("Task doesn't exist! " + taskId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return new ModelAndView<>("task-details.html", new TaskDetails(task, vehicle, session));
    }

    @GET
    @Path("{id}/tasks/{taskId}/log")
    public Response downloadTaskLog(@PathParam("taskId") String taskId) {
        fleetmanagement.backend.tasks.Task task = tasks.tryFindById(UUID.fromString(taskId));
        if (task == null) {
            logger.error("Task doesn't exist! " + taskId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        TaskLogFile logfile = new TaskLogFile(task);
        return Response.ok(logfile.getContent(), "text/plain;charset=utf-8")
                .header("content-disposition", "attachment; filename=" + logfile.getFilename()).build();
    }

    @GET
    @Path("/{id}/install-package")
    public ModelAndView<VehicleDetails> showInstallPackageModal(@PathParam("id") String id) {
        fleetmanagement.backend.vehicles.Vehicle vehicle = vehicles.tryFindById(UUID.fromString(id));
        if (vehicle == null) {
            logger.error("Vehicle doesn't exist! " + id);
            throw new WebApplicationException(Status.NOT_FOUND);
        }
        return showVehicleDetailsWithOptionalStatusMessageAndModal(id, null,
                new InstallPackageModal(vehicle, packages.listAll(), tasks, session));
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/{id}/install-package")
    @UserRoleRequired
    public Response startPackageInstallation(@PathParam("id") String id, @FormParam("package") String packageId) {
        fleetmanagement.backend.vehicles.Vehicle vehicle = vehicles.tryFindById(UUID.fromString(id));

        if (vehicle == null) {
            logger.error("Vehicle doesn't exist! " + id);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        Package pkg = null;
        StartInstallationResult installationResult = null;
        if (packageId != null && packageId.length() > 0) {
            pkg = packages.tryFindById(UUID.fromString(packageId));
            installationResult = installPackage.startInstallation(pkg, vehicle,
                    session.getSecurityContext().getUserPrincipal().getName());
        }

        if (pkg == null || installationResult.startedTasks.isEmpty())
            return Redirect.to("/vehicles/" + id + "/installation-error");
        else
            return Redirect.to("/vehicles/" + id + "/installation-started/" + packageId);
    }

    @GET
    @Path("/{id}/installation-started/{packageId}")
    public ModelAndView<VehicleDetails> showPackageInstallationSuccess(
            @PathParam("id") String id, @PathParam("packageId") String packageId) {
        Package pkg = packages.tryFindById(UUID.fromString(packageId));
        return showVehicleDetailsWithOptionalStatusMessageAndModal(
                id, new SuccessMessage(i18n("installing_on_vehicle", Name.of(pkg, session))), null);
    }

    @GET
    @Path("/{id}/installation-error")
    public ModelAndView<VehicleDetails> showPackageInstallationError(@PathParam("id") String id) {
        return showVehicleDetailsWithOptionalStatusMessageAndModal(
                id, new ErrorMessage(i18n("select_package_for_installation")), null);
    }

    @GET
    @Path("/{id}/task-cancelled/{taskId}")
    public ModelAndView<VehicleDetails> showTaskCancelSuccess(
            @PathParam("id") String id, @PathParam("taskId") String taskId) {
        Task task = tasks.tryFindById(UUID.fromString(taskId));
        return showVehicleDetailsWithOptionalStatusMessageAndModal(id, new SuccessMessage(i18n(
                "cancelled_installation_of", "\"" + Name.of(task.getPackage(), session) + "\"")), null);
    }

    @GET
    @Path("/{id}/delete")
    @UserRoleRequired
    public Response deleteVehicle(@PathParam("id") String id) {
        deleteVehicle.deleteById(UUID.fromString(id));
        return Redirect.to("/vehicles");
    }


    @GET
    @Path("/{id}/diagnosis")
    public ModelAndView<Object> showDiagnosisDetails(@PathParam("id") String id) {
        UUID vehicleId = UUID.fromString(id);
        Diagnosis diagnosis = diagnoses.tryFindByVehicleId(vehicleId);
        Vehicle vehicle = vehicles.tryFindById(vehicleId);

        if (vehicle == null)
            throw new NotFoundException("Vehicle doesn't exist! " + id);

        return new ModelAndView<>("diagnosis-details.html", new DiagnosisDetails(diagnosis, vehicle, licence, session));
    }



    @GET
    @Path("/{id}/diagnosis/{deviceId}")
    public ModelAndView<Object> showDiagnosisDevice(@PathParam("id") String id, @PathParam("deviceId") String deviceId) {
        UUID vehicleId = UUID.fromString(id);
        Vehicle vehicle = vehicles.tryFindById(vehicleId);
        if (vehicle == null)
            throw new NotFoundException("Vehicle doesn't exist! " + id);

        return new ModelAndView<>("diagnosis-device-errors.html",
                new DiagnosedDeviceModel(diagnoses, vehicleId, deviceId, session, licence));
    }

    @GET
    @Path("{id}/enable-auto-sync")
    public Response enableAutoSync(@PathParam("id") String id) {
        vehicles.update(UUID.fromString(id), v -> {
            if (v != null)
                v.autoSync = true;
        });
        return Redirect.to("/vehicles/" + id);
    }

    @GET
    @Path("{id}/disable-auto-sync")
    public Response disableAutoSync(@PathParam("id") String id) {
        vehicles.update(UUID.fromString(id), v -> {
            if (v != null)
                v.autoSync = false;
        });
        return Redirect.to("/vehicles/" + id);
    }

    @GET
    @Path("{id}/edit-name/{name}")
    public Response editName(@PathParam("id") String id, @PathParam("name") String name) {
        Vehicle vehicle = vehicles.update(UUID.fromString(id), v -> {
            if (v != null) {
                if (name.equals("null"))
                    v.setName(v.uic);
                else
                    v.setName(name);
            }
        });
        return Response.ok(vehicle.getName()).build();
    }

    private ModelAndView<VehicleDetails> showVehicleDetailsWithOptionalStatusMessageAndModal(
            String vehicleId, StatusMessage message, InstallPackageModal modal) {
        fleetmanagement.backend.vehicles.Vehicle vehicle = vehicles.tryFindById(UUID.fromString(vehicleId));

        if (vehicle == null) {
            logger.error("Vehicle doesn't exist! " + vehicleId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        VehicleDetails vm = new ListVehicleDetails(groups, packages, tasks, licence).listVehicleDetails(vehicle, session);

        vm.showGeo = licence.isVehicleGeoAvailable();

        vm.message = message;
        vm.installPackageModal = modal;
        return new ModelAndView<>("vehicle-details.html", vm);
    }
}