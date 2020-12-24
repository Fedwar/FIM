package fleetmanagement.usecases;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.DiagnosticSummary;
import fleetmanagement.backend.vehicles.DiagnosticSummary.DiagnosticSummaryType;
import fleetmanagement.backend.vehicles.LiveInformation.Position;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleVersions.Versioned;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.LiveInformationViewModel;
import fleetmanagement.frontend.model.Name;
import fleetmanagement.frontend.model.VehicleDetails;
import fleetmanagement.frontend.model.VehicleDetails.CompletedTask;
import fleetmanagement.frontend.model.VehicleDetails.ComponentVersion;
import fleetmanagement.frontend.model.VehicleDetails.RunningTask;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class ListVehicleDetails {

    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final GroupRepository groups;
    private final PackageRepository packages;
    private final TaskRepository tasks;
    private final Licence licence;

    public ListVehicleDetails(GroupRepository groups, PackageRepository packages, TaskRepository tasks, Licence licence) {
        this.groups = groups;
        this.packages = packages;
        this.tasks = tasks;
        this.licence = licence;
    }

    public VehicleDetails listVehicleDetails(Vehicle v, UserSession request) {
        VehicleDetails vm = new VehicleDetails();
        vm.uic = v.uic;
        vm.additional_uic = (v.additional_uic == null ? "" : v.additional_uic);
        vm.id = v.id.toString();
        vm.name = v.getName();
        vm.autoSync = v.autoSync;
        vm.clientVersion = v.clientVersion;
        vm.lastSeen = v.lastSeen;
        vm.showGeo = licence.isVehicleGeoAvailable();
        vm.showDiagnosis = licence.isDiagnosisInfoAvailable();
        vm.showIpAddress = licence.isVehicleIpAvailable();
        vm.showOperationInfo = licence.isOperationInfoAvailable();
        vm.showAutoSync = licence.isAutoPackageSyncAvailable();
        vm.ipAddress = (v.ipAddress == null ? I18n.get(request, "vehicle_ip_not_available") : v.ipAddress);

        List<Group> sortedGroupsList = new ArrayList<>(groups.listAll());
        sortedGroupsList.sort(Comparator.comparing(g -> g.name));
        sortedGroupsList.stream().filter(group -> !group.id.toString().equals(v.getGroupId()))
                .forEach(group -> vm.groupsForAssigning.put(group.id.toString(), group.name));
        vm.groupId = v.getGroupId();
        if (v.getGroupId() != null) {
            Group g = groups.tryFindById(UUID.fromString(v.getGroupId()));
            if (g != null) {
                vm.groupName = g.name;
            }
        }
        if (v.liveInformation != null) {
            LiveInformationViewModel live = new LiveInformationViewModel(v.liveInformation, request);
            vm.routeInformation = live.routeSummary;
            vm.lastLiveInfoUpdate = live.lastUpdatedAgo;
            Position pos = v.liveInformation.position;
            if (pos != null) {
                vm.latitude = Double.toString(pos.latitude);
                vm.longitude = Double.toString(pos.longitude);
            }
        }

        Set<Versioned> versions = v.versions.getAll();
        if (versions.stream().noneMatch(versioned -> versioned.type == PackageType.DataSupply)
                && licence.isPackageTypeAvailable(PackageType.DataSupply))
            versions.add(new Versioned(PackageType.DataSupply, I18n.get(request, "vehicle_no_data_supply_installed")));

        List<Versioned> versionsSorted = versions.stream()
                .sorted(Comparator.comparing(Versioned::getType).thenComparing(o -> (o.slot == 0 ? Integer.MAX_VALUE : o.slot)))
                .collect(toList());

        for (Versioned versioned : versionsSorted) {
            ComponentVersion version = new ComponentVersion();
            version.component = Name.of(versioned, request);
            version.slot = (versioned.slot == 0 ? "" :
                    String.format("(%s %s)", I18n.get(request, "package_details_slot"), versioned.slot));
            version.version = versioned.version;
            if (versioned.validityBegin != null && versioned.validityEnd != null) {
                String begin = ZonedDateTime.parse(versioned.validityBegin)
                        .format(dateFormat.ofLocalizedDateTime(FormatStyle.SHORT));
                String end = ZonedDateTime.parse(versioned.validityEnd)
                        .format(dateFormat.ofLocalizedDateTime(FormatStyle.SHORT));
                version.validity = begin + " - " + end;
            }
            version.active = versioned.active;
            if (version.version != null) {
                Package pkg = findPackageForVehicleComponentVersion(versioned);
                if (pkg != null)
                    version.packageId = pkg.id.toString();
            }
            vm.versions.add(version);
        }

        DiagnosticSummary summary = v.getDiagnosticSummary(ZonedDateTime.now());
        if (summary.type != DiagnosticSummaryType.Ok) {
            vm.diagnosticError = Name.of(summary, request);
        }

        for (Task backendTask : v.getTasks(tasks)) {
            if (backendTask.isCompleted()) {
                vm.completedTasks.add(CompletedTask.create(backendTask, request));
            } else {
                vm.runningTasks.add(RunningTask.create(backendTask, request));
            }
        }
        vm.runningTasks.sort((task1, task2) -> -task1.startDate.compareTo(task2.startDate));
        vm.completedTasks.sort((task1, task2) -> -task1.completionDate.compareTo(task2.completionDate));
        vm.completedTasks.removeIf(task -> vm.completedTasks.indexOf(task) > 2);
        return vm;
    }

    private Package findPackageForVehicleComponentVersion(Versioned versioned) {
        String version = versioned.version;
        switch (versioned.type) {
            case DataSupply:
                return packages.listByType(PackageType.DataSupply).stream().filter(p -> p.version.equals(version) && p.slot == versioned.slot).findFirst().orElse(null);

            default:
                return packages.listByType(versioned.type).stream().filter(p -> p.version.equals(version)).findFirst().orElse(null);
        }
    }

}
