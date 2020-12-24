package fleetmanagement.frontend.controllers;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.ActivityLog;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.repositories.exception.LocalizableException;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskLogFile;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.ConflictingTasksModal;
import fleetmanagement.frontend.model.GroupList;
import fleetmanagement.frontend.model.InstallPackageModal;
import fleetmanagement.frontend.model.PackageDetails;
import fleetmanagement.frontend.model.PackageInstallationLogs;
import fleetmanagement.frontend.model.PackageList;
import fleetmanagement.frontend.model.PackageTaskList;
import fleetmanagement.frontend.model.PackagesAndGroups;
import fleetmanagement.frontend.model.StatusMessage;
import fleetmanagement.frontend.model.StatusMessage.ErrorMessage;
import fleetmanagement.frontend.model.StatusMessage.SuccessMessage;
import fleetmanagement.frontend.model.UploadPackageViewModel;
import fleetmanagement.frontend.security.webserver.UserRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.usecases.DeletePackage;
import fleetmanagement.usecases.ImportPackage;
import fleetmanagement.usecases.InstallPackage;
import fleetmanagement.usecases.InstallPackage.StartInstallationResult;
import fleetmanagement.usecases.ListPackageDetails;
import fleetmanagement.usecases.ListPackages;
import gsp.util.DoNotObfuscate;
import org.apache.commons.lang3.StringUtils;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Path("/packages")
@Component
public class Packages extends FrontendController {

    static final String INSTALLATION_TASK_COUNT_PARAM = "ir";
    static final String INSTALLATION_MESSAGE_PARAM = "m";

    private final ImportPackage importer;
    private final DeletePackage deleter;
    private final PackageRepository packages;
    private final VehicleRepository vehicles;
    private final GroupRepository groups;
    private final TaskRepository tasks;
    private final InstallPackage pkgInstallation;
    private final Licence licence;

    private static final Logger logger = Logger.getLogger(Packages.class);

    @Autowired
    public Packages(UserSession session, PackageRepository repository, VehicleRepository vehicles, GroupRepository groups, TaskRepository tasks, InstallPackage installer, ImportPackage importer, DeletePackage deleter, Licence licence) {
        super(session);
        this.packages = repository;
        this.importer = importer;
        this.deleter = deleter;
        this.vehicles = vehicles;
        this.groups = groups;
        this.tasks = tasks;
        this.pkgInstallation = installer;
        this.licence = licence;
    }

    @GET
    public ModelAndView<PackagesAndGroups> getListPackagesUI() {
        return new ModelAndView<>("package-list.html",
                new PackagesAndGroups(
                        new ListPackages(packages.listAll(), vehicles, tasks).listPackages(session, groups),
                        new GroupList(groups.listAll(), null, vehicles)));
    }


    @GET
    @Path("group/{groupId}")
    public ModelAndView<PackagesAndGroups> filteredList(@PathParam("groupId") String groupId) {
        if (groupId == null)
            throw new IllegalArgumentException("Invalid argument of group/{groupId}. groupId cannot be null.");

        List<Package> packageList = packages.listAll().stream()
                .filter(x -> x.groupId != null && x.groupId.equals(UUID.fromString(groupId)))
                .collect(toList());

        return new ModelAndView<>("package-list.html",
                new PackagesAndGroups(
                        new ListPackages(packageList, vehicles, tasks).listPackages(session, groups),
                        new GroupList(groups.listAll(), groupId, vehicles)));

    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @UserRoleRequired
    public Response upload(
            @FormDataParam("package") InputStream data,
            @FormDataParam("package") FormDataContentDisposition meta,
            @FormDataParam("package-name") String packageName) {
        String filename = (packageName != null && !packageName.isEmpty()) ? packageName : meta.getFileName();
        filename = extractFilename(filename);
        try {
            Package imported = importer.importPackage(filename, data, "User: " + session.getSecurityContext().getUserPrincipal().getName(), session.getSecurityContext().getUserPrincipal().getName());
            ActivityLog.packageMessage(ActivityLog.Operations.PACKAGE_IMPORTED, null, filename, imported, session.getSecurityContext().getUserPrincipal().getName());
            return Response.created(new URI(imported.id.toString())).build();
        } catch (Exception e) {
            Error jsonError = localize(e, session.getLocale());
            ActivityLog.groupFileMessage(ActivityLog.Operations.CANNOT_IMPORT_PACKAGE, null, filename, session.getSecurityContext().getUserPrincipal().getName());
            return Response.serverError().type(MediaType.APPLICATION_JSON_TYPE).entity(jsonError).build();
        }
    }

    private Error localize(Exception e, Locale locale) {
        if (e instanceof LocalizableException) {
            return new Error((LocalizableException) e, locale);
        }
        return new Error(e);
    }

    private String extractFilename(String filename) {
        int lastSlash = StringUtils.lastIndexOfAny(filename, "/", "\\");
        if (lastSlash == -1)
            return filename;

        return filename.substring(lastSlash + 1);
    }

    @GET
    @Path("/upload")
    @UserRoleRequired
    public ModelAndView<UploadPackageViewModel> getUploadPackageUI() {
        return new ModelAndView<>("package-upload.html", new UploadPackageViewModel());
    }

    @GET
    @Path("/delete")
    @UserRoleRequired
    public Response deletePackage(@QueryParam("key") String key) {
        deleter.deleteById(UUID.fromString(key), session.getSecurityContext().getUserPrincipal().getName());
        return Redirect.to("/packages");
    }

    @GET
    @Path("/{package-id}")
    public ModelAndView<PackageDetails> getPackageDetailsUI(@PathParam("package-id") String packageId,
                                                            @QueryParam(INSTALLATION_TASK_COUNT_PARAM) Integer taskCount,
                                                            @QueryParam(INSTALLATION_MESSAGE_PARAM) String message) throws UnsupportedEncodingException {
        Package p = packages.tryFindById(UUID.fromString(packageId));

        if (p == null) {
            logger.error("Package doesn't exist! " + packageId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        PackageDetails vm = new ListPackageDetails(vehicles, tasks, groups, packages).listPackageDetails(p, session);

        if (message != null && !message.isEmpty()) {
            vm.message = new ErrorMessage(URLDecoder.decode(message, "UTF-8"));
        } else if (taskCount != null) {
            if (taskCount == 0) {
                vm.message = new ErrorMessage(i18n("select_vehicle_for_installation"));
            } else {
                vm.message = new SuccessMessage(i18n("installing_on_x_vehicles", taskCount));
            }
            if (p.installation != null && p.installation.getConflictingTasks().size() > 0) {
                vm.conflictingTasksModal = new ConflictingTasksModal(p, p.installation.getConflictingTasks(), vehicles, session);
            }
        }

        return new ModelAndView<>("package-details.html", vm);
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("/{package-id}/content")
    @UserRoleRequired
    public Response downloadPackage(@PathParam("package-id") String packageId) throws FileNotFoundException {
        Package p = packages.tryFindById(UUID.fromString(packageId));

        if (p == null) {
            logger.error("Package doesn't exist! " + packageId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return Response.ok()
                .header("content-disposition", "attachment; filename=" + p.archive.getName()).
                        entity(new FileInputStream(p.archive)).build();
    }

    @GET
    @Path("{id}/tasks")
    public ModelAndView<PackageTaskList> allTasksList(@PathParam("id") String id) {
        Package p = packages.tryFindById(UUID.fromString(id));

        if (p == null) {
            logger.error("Package doesn't exist! " + id);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return new ModelAndView<>("package-alltaskslist.html", new PackageTaskList(p, tasks, vehicles, session));
    }

    @GET
    @Path("{id}/logs")
    public ModelAndView<PackageInstallationLogs> installationLogs(@PathParam("id") String packageId) {
        Package p = packages.tryFindById(UUID.fromString(packageId));

        if (p == null) {
            logger.error("Package doesn't exist! " + packageId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        List<Task> taskList = p.installation == null ? Collections.emptyList()
                : p.installation.getTasks().stream()
                .map(tasks::tryFindById)
                .filter(Objects::nonNull)
                .collect(toList());
        PackageInstallationLogs logs = new PackageInstallationLogs(p, taskList, vehicles, session);
        return new ModelAndView<>("package-logs.html", logs);
    }

    @GET
    @Path("{id}/logs/download")
    public Response downloadInstallationLog(@PathParam("id") String packageId) {
        Package p = packages.tryFindById(UUID.fromString(packageId));

        if (p == null) {
            logger.error("Package doesn't exist! " + packageId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        List<Task> taskList = p.installation == null ? Collections.emptyList()
                : p.installation.getTasks().stream()
                .map(tasks::tryFindById)
                .filter(Objects::nonNull)
                .collect(toList());

        TaskLogFile logfile = new TaskLogFile(taskList, "package-" + packageId + "-installation-log.txt", vehicles);
        return Response.ok(logfile.getContent(), "text/plain;charset=utf-8")
                .header("content-disposition", "attachment; filename=" + logfile.getFilename()).build();
    }

    @GET
    @Path("{package-id}/ajax")
    public ModelAndView<PackageDetails> getPackageDetailsAjax(@PathParam("package-id") String packageId) {
        Package p = packages.tryFindById(UUID.fromString(packageId));

        if (p == null) {
            logger.error("Package doesn't exist! " + packageId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        PackageDetails vm = new ListPackageDetails(vehicles, tasks, groups, packages).listPackageDetails(p, session);
        return new ModelAndView<>("package-details-template.html", vm);
    }

    @GET
    @Path("ajax")
    public ModelAndView<PackageList> getListPackagesAjax() {
        return new ModelAndView<>("package-list-template.html",
                new ListPackages(packages.listAll(), vehicles, tasks).listPackages(session, groups));
    }

    @GET
    @Path("group/{groupId}/ajax")
    public ModelAndView<PackageList> filteredListAjax(@PathParam("groupId") String groupId) {
        if (groupId == null)
            throw new IllegalArgumentException("Invalid argument of group/{groupId}. groupId cannot be null.");

        List<Package> packageList = packages.stream()
                .filter(x -> x.groupId != null && x.groupId.equals(UUID.fromString(groupId)))
                .collect(toList());

        return new ModelAndView<>("package-list-template.html",
                new ListPackages(packageList, vehicles, tasks).listPackages(session, groups));

    }

    @GET
    @Path("{package-id}/cancel-all")
    public ModelAndView<PackageDetails> cancelAllRunningTasks(@PathParam("package-id") String packageId) {
        Package pkg = packages.tryFindById(UUID.fromString(packageId));

        if (pkg == null) {
            logger.error("Package doesn't exist! " + packageId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        for (Task task : tasks.getRunningTasksByPackage(UUID.fromString(packageId))) {
            ActivityLog.vehicleMessage(ActivityLog.Operations.INSTALLATION_CANCELLED, pkg,
                    vehicles.tryFindById(task.getVehicleId()), session.getSecurityContext().getUserPrincipal().getName());
            tasks.update(UUID.fromString(task.getId().toString()), toCancel -> {
                if (toCancel == null)
                    throw new WebApplicationException(Status.NOT_FOUND);

                toCancel.cancel();
            });
        }

        return showPackageDetailsWithOptionalStatusMessageAndModal(
                packageId,
                new SuccessMessage(
                        i18n(
                                "cancelled_installation_of",
                                "\"" + pkg.type + " " + pkg.version + "\" " + i18n("at_all_vehicles")
                        )
                ),
                null
        );
    }

    private ModelAndView<PackageDetails> showPackageDetailsWithOptionalStatusMessageAndModal(
            String packageId, StatusMessage message, InstallPackageModal modal) {
        fleetmanagement.backend.packages.Package pkg = packages.tryFindById(UUID.fromString(packageId));

        if (pkg == null) {
            logger.error("Package doesn't exist! " + packageId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        PackageDetails vm = new ListPackageDetails(vehicles, tasks, groups, packages).listPackageDetails(pkg, session);
        vm.message = message;

        return new ModelAndView<>("package-details.html", vm);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/{package-id}/start-installation")
    @UserRoleRequired
    public Response startPackageInstallation(@PathParam("package-id") String packageId, @FormParam("vehicles") List<String> targetVehicleIds)
            throws UnsupportedEncodingException {
        Package p = packages.tryFindById(UUID.fromString(packageId));

        if (p == null) {
            logger.error("Package doesn't exist! " + packageId);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        String parameters = "";

        try {
            List<Vehicle> targetVehicles = targetVehicleIds.stream().map(x -> vehicles.tryFindById(UUID.fromString(x))).filter(Objects::nonNull).collect(toList());
            StartInstallationResult result = pkgInstallation.startInstallation(p, targetVehicles,
                    session.getSecurityContext().getUserPrincipal().getName());
            parameters = INSTALLATION_TASK_COUNT_PARAM + "=" + result.startedTasks.size();
        } catch (Exception e) {
            Error jsonError = localize(e, session.getLocale());
            parameters = INSTALLATION_MESSAGE_PARAM + "=" + URLEncoder.encode(jsonError.errorMessage, "UTF-8");
        }

        return Redirect.to("/packages/" + packageId + "?" + parameters);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("/{package-id}/cancel-tasks")
    @UserRoleRequired
    public Response cancelConflictingTasks(@PathParam("package-id") String packageId, @FormParam("tasks") List<String> taskIds) {

        for (String taskId : taskIds) {
            Task task = tasks.tryFindById(UUID.fromString(taskId));
            if (task == null) {
                logger.warn("Unknown id of task which is supposed to be cancelled! " + taskId);
                continue;
            }
            ActivityLog.vehicleMessage(ActivityLog.Operations.INSTALLATION_CANCELLED, task.getPackage(),
                    vehicles.tryFindById(task.getVehicleId()),
                    session.getSecurityContext().getUserPrincipal().getName());
            tasks.update(UUID.fromString(taskId), toCancel -> {
                if (toCancel != null)
                    toCancel.cancel();
            });
        }

        return Redirect.to("/packages/" + packageId);
    }

    @DoNotObfuscate
    public static class Error {
        public String errorMessage;
        public String callstack;

        public Error(String message) {
            this.errorMessage = message;
            this.callstack = null;
        }

        public Error(Exception e) {
            this.errorMessage = e.getMessage();
            this.callstack = e.toString();
        }

        public Error(LocalizableException e, Locale locale) {
            this.errorMessage = e.getLocalizedMessage(locale);
            this.callstack = e.toString();
        }
    }
}
