package fleetmanagement.frontend.controllers;

import com.google.gson.Gson;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupInstaller;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.groups.GroupsWatcher;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.repositories.exception.GroupDuplicationException;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.security.webserver.ConfigRoleRequired;
import fleetmanagement.frontend.security.webserver.UserRoleRequired;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Path("groups")
@Component
public class Groups extends FrontendController {

    private static final Gson gson = new Gson();
    private static final Logger logger = Logger.getLogger(Groups.class);
    private final GroupRepository groups;
    private final VehicleRepository vehicles;
    private final GroupsWatcher watcher;
    private final PackageRepository packages;
    private final GroupInstaller groupInstaller;

    @Autowired
    public Groups(
            UserSession session,
            GroupRepository groups,
            VehicleRepository vehicles,
            GroupsWatcher watcher,
            PackageRepository packages,
            GroupInstaller groupInstaller
    ) {
        super(session);
        this.groups = groups;
        this.vehicles = vehicles;
        this.watcher = watcher;
        this.packages = packages;
        this.groupInstaller = groupInstaller;
    }

    @POST
    @ConfigRoleRequired
    public Response addGroup(String json) {
        Group group = gson.fromJson(json, Group.class);
        if (group == null) {
            logger.error("Can't get group from client data! " + json);
            throw new IllegalArgumentException("Can't get group from client data! " + json);
        }
        if (group.dir == null) {
            logger.error("Can't get group folder from client data! " + json);
            throw new IllegalArgumentException("Can't get group folder from client data! " + json);
        }
        group.isAutoSyncEnabled = true;

        File groupDir = watcher.getGroupDir(group);
        try {
            if (watcher.verifyDir(groupDir, group)) {
                groups.insert(group);
                List<File> groupFiles = watcher.watchDir(group);
                if (groupFiles == null)
                    logger.error("Can't watch for directory (" + group.dir + ") of group \"" + group.name);
            } else {
                return Response.status(Response.Status.CONFLICT)
                        .entity(i18n("group.error.group_wrong_path"))
                        .build();
            }
        } catch (GroupDuplicationException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(i18n("group.error.name_already_exists"))
                    .build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("{id}")
    @ConfigRoleRequired
    public Response editGroup(@PathParam("id") String id, String json) {
        Group oldGroup = groups.tryFindById(UUID.fromString(id));
        if (oldGroup == null) {
            logger.warn("Can't change a not existing group! Group ID = " + id);
            return Redirect.to("/vehicles");
        }
        Group newGroup = gson.fromJson(json, Group.class);
        newGroup.id = UUID.fromString(id);

        if (!oldGroup.name.equals(newGroup.name)) {
            Optional<Group> byName = groups.findByName(newGroup.name);
            if (byName.isPresent()) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(i18n("group.error.name_already_exists"))
                        .build();
            }
        }

        File groupDir = watcher.getGroupDir(newGroup);
        if (watcher.verifyDir(groupDir, newGroup)) {
            List<File> groupFiles = watcher.watchDir(newGroup);
            if (groupFiles == null) {
                logger.error("Can't watch for directory (" + newGroup.dir + ") of group \"" + newGroup.name);
                return Redirect.to("/404");
            }
            groups.update(UUID.fromString(id), group -> {
                group.name = newGroup.name;
                group.dir = newGroup.dir;
            });
            if (!newGroup.dir.equals(oldGroup.dir)) {
                if (!watcher.stopWatching(oldGroup)) {
                    logger.warn("Failed to stop watching the group directory.");
                }
            }
        } else {
            return Response.status(Response.Status.CONFLICT)
                    .entity(i18n("group.error.group_wrong_path"))
                    .build();
        }
        return Response.ok().build();
    }

    @POST
    @Path("{packageId}/assign-package")
    @UserRoleRequired
    public Response assignPackage(@PathParam("packageId") String packageId, String groupsJson) {
        List<String> groupsIds = gson.fromJson(groupsJson, List.class);
        for (String groupId : groupsIds) {
            Group group = groups.tryFindById(UUID.fromString(groupId));
            if (group == null) {
                logger.warn("Can't assign package to a not existing group! Group ID = " + groupId);
                continue;
            }
            Package pkg = packages.tryFindById(UUID.fromString(packageId));
            groupInstaller.assignPackageToGroup(pkg, group);
        }
        return Redirect.to("/packages/" + packageId);
    }

    @POST
    @Path("{packageId}/remove-package")
    @UserRoleRequired
    public Response removePackageFromGroups(@PathParam("packageId") String packageId, String groupsJson) {
        List<String> groupsIds = gson.fromJson(groupsJson, List.class);
        for (String groupId : groupsIds) {
            Group group = groups.tryFindById(UUID.fromString(groupId));
            if (group == null) {
                logger.warn("Can't remove package from a not existing group! Group ID = " + groupId);
                continue;
            }
            Package pkg = packages.tryFindById(UUID.fromString(packageId));
            groupInstaller.removePackageFromGroup(pkg, group, session.getSecurityContext().getUserPrincipal().getName());
        }
        return Redirect.to("/packages");
    }

    @POST
    @Path("{vehicleId}/assign-vehicle")
    @UserRoleRequired
    public Response assignVehicle(@PathParam("vehicleId") String vehicleId, String groupsJson) {
        List<String> groupsIds = gson.fromJson(groupsJson, List.class);
        for (String groupId : groupsIds) {
            Group group = groups.tryFindById(UUID.fromString(groupId));
            if (group == null) {
                logger.warn("Can't assign vehicle to a not existing group! Group ID = " + groupId);
                continue;
            }
            Vehicle vehicle = vehicles.tryFindById(UUID.fromString(vehicleId));
            groupInstaller.assignVehicles(group, Collections.singletonList(vehicle));
        }
        return Redirect.to("/vehicles/" + vehicleId);
    }

    @POST
    @Path("{id}/assign-vehicles")
    @UserRoleRequired
    public Response assignVehicles(@PathParam("id") String id, String json) {
        Group group = groups.tryFindById(UUID.fromString(id));
        if (group == null) {
            logger.warn("Can't assign vehicles to not existing group! Group ID = " + id);
            return Redirect.to("/vehicles");
        }
        List<String> vehiclesIds = gson.fromJson(json, List.class);
        List<Vehicle> vehiclesAssigned = vehiclesIds.stream().map(UUID::fromString)
                .map(vehicles::tryFindById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        groupInstaller.assignVehicles(group, vehiclesAssigned);

        return Redirect.to("/vehicles");
    }

    @POST
    @Path("{id}/assign-packages")
    @UserRoleRequired
    public Response assignPackages(@PathParam("id") String id, String json) {
        Group group = groups.tryFindById(UUID.fromString(id));
        if (group == null) {
            logger.warn("Can't install packages to vehicles in a not existing group! Group ID = " + id);
            return Redirect.to("/packages");
        }

        List<String> packagesIds = gson.fromJson(json, List.class);
        for (String packageId : packagesIds) {
            Package pkg = packages.tryFindById(UUID.fromString(packageId));
            groupInstaller.assignPackageToGroup(pkg, group);
        }
        return Redirect.to("/packages");
    }

    @POST
    @Path("remove-vehicles")
    @UserRoleRequired
    public Response removeVehiclesFromGroup(String json) {
        List<String> vehiclesIds = gson.fromJson(json, List.class);
        for (String vehicleId : vehiclesIds) {
            logger.debug("VehicleId = " + vehicleId);
            vehicles.update(UUID.fromString(vehicleId), v -> {
                if (v == null)
                    return;

                v.setGroupId(null);
            });
        }

        return Redirect.to("/vehicles");
    }

    @DELETE
    @Path("{id}")
    @ConfigRoleRequired
    public Response deleteGroup(@PathParam("id") String id) {
        Group group = groups.tryFindById(UUID.fromString(id));
        if (group == null) {
            logger.warn("Can't delete group which doesn't exist! Group ID = " + id);
            return Redirect.to("/vehicles");
        }

        for (Vehicle vehicle : vehicles.listByGroup(id)) {
            vehicles.update(vehicle.id, v -> {
                if (v != null)
                    v.setGroupId(null);
            });
        }

        if (!watcher.stopWatching(group))
            logger.warn("Failed to stop watching the group directory.");

        groupInstaller.removeAllPackagesByGroupId(group.id);
        groups.delete(UUID.fromString(id));
        return Redirect.to("/vehicles");
    }
}
