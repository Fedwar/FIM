package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.AdminGC;
import fleetmanagement.frontend.model.Logs;
import fleetmanagement.frontend.security.webserver.ConfigRoleRequired;
import fleetmanagement.frontend.security.webserver.UserRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

@Path("/admin")
@Component
public class Admin extends FrontendController {

    private static final Logger logger = Logger.getLogger(Admin.class);

    private final VehicleRepository vehicleRepository;
    private final TaskRepository tasks;
    private final PackageTypeRepository packageTypeRepository;
    private final GroupRepository groupRepository;
    private final Licence licence;
    private final FimConfig config;

    @Autowired
    public Admin(UserSession session, VehicleRepository vehicleRepository,
                 TaskRepository tasks, PackageTypeRepository packageTypeRepository,
                 GroupRepository groupRepository, Licence licence, FimConfig config) {
        super(session);
        this.vehicleRepository = vehicleRepository;
        this.tasks = tasks;
        this.packageTypeRepository = packageTypeRepository;
        this.groupRepository = groupRepository;
        this.config = config;
        this.licence = licence;
    }

    @GET
    public ModelAndView<fleetmanagement.frontend.model.Admin> getAdminUI() {
        return getAdminLogsUI();
    }

    @GET
    @Path("logs-page")
    public ModelAndView<fleetmanagement.frontend.model.Admin> getAdminLogsUI() {
        fleetmanagement.frontend.model.Admin vm =
                new fleetmanagement.frontend.model.Admin(licence);
        return new ModelAndView<>("admin-logs.html", vm);
    }

    @GET
    @Path("gc")
    @ConfigRoleRequired
    public ModelAndView<AdminGC> getAdminGCUI() {
        AdminGC vm = new AdminGC(packageTypeRepository, licence);
        return new ModelAndView<>("admin-gc.html", vm);
    }

    @GET
    @Path("auto-sync")
    @UserRoleRequired
    public ModelAndView<fleetmanagement.frontend.model.AdminAutoSync> getAdminAutoSyncUI() {
        fleetmanagement.frontend.model.AdminAutoSync vm =
                new fleetmanagement.frontend.model.AdminAutoSync(packageTypeRepository, licence);
        return new ModelAndView<>("admin-autosync.html", vm);
    }

    @GET
    @Path("logs")
    public Response downloadAllLogs() throws IOException {
        Logs logs = new Logs(vehicleRepository, tasks);
        String filename = "FleetManagementLogs_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_kkmm") + ".zip";

        return Response.ok().type("application/x-zip-compressed")
                .header("content-disposition", "attachment; filename=" + filename).
                        entity(logs.getAsZipStream()).build();
    }

    @GET
    @Path("activity-logs")
    public Response downloadActivityJournal() throws IOException {
        String filename = "ActivityJournal_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_kkmm") + ".csv";
        FileInputStream journal = new FileInputStream(config.getDataDirectory().getPath() +
                File.separator + "logs" + File.separator + "data-transfer.csv");

        return Response.ok().type("text/csv")
                .header("content-disposition", "attachment; filename=" + filename).
                        entity(journal).build();
    }

    @POST
    @UserRoleRequired
    public Response updateDataTypes(MultivaluedMap<String, String> params) {
        packageTypeRepository.disableAll();
        Iterator<String> it = params.keySet().iterator();
        String theKey;
        while (it.hasNext()) {
            theKey = it.next();
            PackageType packageType = PackageType.getByResourceKey(theKey);
            packageTypeRepository.enableGC(packageType);
        }
        try {
            packageTypeRepository.save();
        } catch (IOException e) {
            logger.error("Can't save package types!", e);
        }
        return Redirect.to("/admin/gc");
    }

    @POST
    @Path("auto-sync")
    @UserRoleRequired
    public Response updateAutoSync(MultivaluedMap<String, String> params) {
        packageTypeRepository.disableAllAutoSync();
        Iterator<String> it = params.keySet().iterator();
        String theKey;
        while (it.hasNext()) {
            theKey = it.next();
            PackageType packageType = PackageType.getByResourceKey(theKey);
            packageTypeRepository.enableAutoSync(packageType);
        }
        try {
            packageTypeRepository.save();
        } catch (IOException e) {
            logger.error("Can't save package types!", e);
        }
        return Redirect.to("/admin/auto-sync");
    }



}
