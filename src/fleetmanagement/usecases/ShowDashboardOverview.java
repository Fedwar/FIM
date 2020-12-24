package fleetmanagement.usecases;

import fleetmanagement.FleetManagement;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageInstallationStatusOverview;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.FilterSequenceRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.PathComposer;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilter;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterSequence;
import fleetmanagement.backend.vehicles.DiagnosticSummary.DiagnosticSummaryType;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.config.Settings;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.Dashboard;
import fleetmanagement.frontend.model.Dashboard.DataPackets;
import fleetmanagement.frontend.model.Dashboard.DiagnosticError;
import fleetmanagement.frontend.model.Dashboard.RunningInstallation;
import fleetmanagement.frontend.model.Dashboard.Statistics;
import fleetmanagement.frontend.model.FilterDirectory;
import fleetmanagement.frontend.model.Name;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.List;

import static fleetmanagement.backend.vehiclecommunication.upload.filter.FilterType.AD_FILTER_TYPE;

@Component
public class ShowDashboardOverview {

    private final VehicleRepository vehicles;
    private final PackageRepository packages;
    @Autowired
    private GroupRepository groupRepository;
    private final TaskRepository tasks;
    private final FilterSequenceRepository filterSequenceRepository;
    private final Licence licence;
    @Autowired
    private Settings settings;

    @Autowired
    public ShowDashboardOverview(VehicleRepository vehicles, PackageRepository packages, TaskRepository tasks,
                                 FilterSequenceRepository filterSequenceRepository, Licence licence) {
        this.vehicles = vehicles;
        this.packages = packages;
        this.tasks = tasks;
        this.filterSequenceRepository = filterSequenceRepository;
        this.licence = licence;
    }

    public Dashboard createDashboard(UserSession request) {
        Dashboard result = new Dashboard();
        result.licence = licence;
        result.softwareVersion = FleetManagement.getVersion();
        assembleStatistics(result.statistics);
        assembleRunningInstallations(result.runningInstallations, request);
        assembleDiagnosticErrors(result.diagnosticErrors, request);
        assembleDataPackets(result.dataPackets);
        return result;
    }

    private void assembleStatistics(Statistics statistics) {
        List<Package> pkgs = packages.listAll();
        statistics.packages = pkgs.size();
        statistics.vehicles = vehicles.listAll().size();
        statistics.totalPackageSize = FileUtils.byteCountToDisplaySize(pkgs.stream().mapToLong(x -> x.size.bytes).sum());
        statistics.totalPackageFiles = pkgs.stream().mapToInt(x -> x.size.files).sum();
    }

    private void assembleRunningInstallations(List<RunningInstallation> runningInstallations, UserSession request) {
        for (Package p : packages.listAll()) {
            PackageInstallationStatusOverview status = PackageInstallationStatusOverview.create(p, vehicles, tasks);
            if (status.installationInProgress) {
                RunningInstallation install = new RunningInstallation();
                install.name = Name.of(p, request);
                install.packageId = p.id.toString();
                Group g = p.groupId == null ? null : groupRepository.tryFindById(p.groupId);
                if (g != null) {
                    install.packageGroupId = g.id.toString();
                    install.packageGroupName = g.name;
                }
                install.progress = status.progressPercent;
                install.finishedInstallations = status.installed;
                install.totalInstallations = status.total;
                runningInstallations.add(install);
            }
        }
    }

    private void assembleDiagnosticErrors(List<DiagnosticError> diagnosticErrors, UserSession request) {
        for (Vehicle v : vehicles.listAll()) {
            fleetmanagement.backend.vehicles.DiagnosticSummary diagnosticSummary = v.getDiagnosticSummary(ZonedDateTime.now());

            if (diagnosticSummary.type != DiagnosticSummaryType.Ok) {
                DiagnosticError e = new DiagnosticError();
                e.vehicleName = v.getName();
                e.vehicleId = v.id.toString();
                e.description = Name.of(diagnosticSummary, request);
                diagnosticErrors.add(e);
            }
        }
    }

    private void assembleDataPackets(DataPackets filterDirectories) {
        UploadFilterSequence filterSequence = filterSequenceRepository.findByType(AD_FILTER_TYPE);
        int dataPacketsCount = 0;
        for (UploadFilter filter : filterSequence.filters) {
            String cleanPath = PathComposer.getCleanPath(filter);
            FilterDirectory filterDirectory = new FilterDirectory(filter, cleanPath, licence, settings);
            for (FilterDirectory.FileModel dataPacket : filterDirectory.files) {
                if (dataPacket.isDirectory)
                    continue;
                filterSequence.notViewedFiles.stream()
                        .filter(s -> new File(s).equals(new File(dataPacket.path)))
                        .forEach(s -> {
                            filterDirectories.newDataPackets.add(dataPacket);
                        });
                dataPacketsCount++;
            }
            filterDirectories.newDataPacketsCount = filterDirectories.newDataPackets.size();
            filterDirectories.dataPacketsCount = dataPacketsCount;
        }
    }

    public void setGroupRepository(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }
}
