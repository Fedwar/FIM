package fleetmanagement.backend.packages.sync;

import fleetmanagement.TestObjectFactory;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.tasks.TaskStatus;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.config.Licence;
import fleetmanagement.usecases.InstallPackage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PackageSyncServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private PackageTypeRepository packageTypeRepository;
    @Mock
    private Licence licence;
    @Mock
    private InstallPackage installPackage;

    private PackageSyncService tested;

    private Vehicle vehicle = TestObjectFactory.createVehicle("vehicle1");

    @Before
    public void setup() {
        vehicle.autoSync = true;
        for (PackageType packageType : PackageType.values()) {
            when(packageTypeRepository.isAutoSyncEnabled(packageType)).thenReturn(true);
        }
        when(licence.isAutoPackageSyncAvailable()).thenReturn(true);
        tested = new PackageSyncService(taskRepository, packageTypeRepository, licence, installPackage);
    }

    @Test
    public void install_vehiclePackageVersionIsDifferent()  {
        PackageType packageType = PackageType.CopyStick;
        Package pkg = makePackageThatWillBeSynced(packageType);

        tested.syncPackages(vehicle, packageType);

        verify(installPackage).startInstallation(pkg, vehicle, null);
    }

    @Test
    public void noInstall_vehiclePackageVersionIsSame()  {
        PackageType packageType = PackageType.OebbDigitalContent;
        Package pkg = makePackageThatWillBeSynced(packageType);
        vehicle.versions.set(packageType, "v2");

        tested.syncPackages(vehicle, packageType);

        verifyZeroInteractions(installPackage);
    }

    @Test
    public void noInstall_taskInProgress()  {
        PackageType packageType = PackageType.CopyStick;
        Package pkg = makePackageThatWillBeSynced(packageType, TaskStatus.ServerStatus.Running);

        tested.syncPackages(vehicle, packageType);

        verifyZeroInteractions(installPackage);
    }

    @Test
    public void noInstall_taskCancelled()  {
        PackageType packageType = PackageType.CopyStick;
        Package pkg = makePackageThatWillBeSynced(packageType, TaskStatus.ServerStatus.Running);

        tested.syncPackages(vehicle, packageType);

        verifyZeroInteractions(installPackage);
    }

    @Test
    public void install_forEachSlot() {
        PackageType packageType = PackageType.DataSupply;
        vehicle.versions.set(packageType, "v1", 0, null, null, false);
        vehicle.versions.set(packageType, "v1", 1, null, null, false);
        List<Package> packages = makePackageThatWillBeSynced(packageType, TaskStatus.ServerStatus.Finished, 0, 1);

        tested.syncPackages(vehicle, packageType);

        assertThat(packages.size(), is(2));
        verify(installPackage).startInstallation(packages.get(0), vehicle, null);
        verify(installPackage).startInstallation(packages.get(1), vehicle, null);
    }

    @Test
    public void noSyncIfNotLicenced()  {
        when(licence.isAutoPackageSyncAvailable()).thenReturn(false);

        PackageType packageType = PackageType.CopyStick;
        Package pkg = makePackageThatWillBeSynced(packageType);

        tested.syncPackages(vehicle, pkg.type);

        verifyZeroInteractions(installPackage);
    }

    @Test
    public void noSyncIfVehicleAutosyncDisabled()  {
        vehicle.autoSync = false;

        PackageType packageType = PackageType.CopyStick;
        Package pkg = makePackageThatWillBeSynced(packageType);

        tested.syncPackages(vehicle, pkg.type);

        verifyZeroInteractions(installPackage);
    }

    @Test
    public void noSyncIfPackageTypeAutosyncDisabled() {
        PackageType packageType = PackageType.CopyStick;
        when(packageTypeRepository.isAutoSyncEnabled(packageType)).thenReturn(false);

        Package pkg = makePackageThatWillBeSynced(packageType);

        tested.syncPackages(vehicle, pkg.type);

        verifyZeroInteractions(installPackage);
    }

    public Package makePackageThatWillBeSynced(PackageType packageType) {
        return makePackageThatWillBeSynced(packageType, TaskStatus.ServerStatus.Finished, (Integer) null).get(0);
    }

    public Package makePackageThatWillBeSynced(PackageType packageType, TaskStatus.ServerStatus taskStatus) {
        return makePackageThatWillBeSynced(packageType, taskStatus, (Integer) null).get(0);
    }

    public List<Package> makePackageThatWillBeSynced(PackageType packageType, TaskStatus.ServerStatus taskStatus, Integer... slots) {

        List<Package> packages = new ArrayList<>();
        Map<Integer, Task> slotMap = new HashMap<>();
        for (Integer slot : slots) {
            vehicle.versions.set(packageType, "v1");

            Package pkg = TestObjectFactory.createPackage(packageType, "v2", slot, null, null);
            packages.add(pkg);

            Task t = new Task(pkg, vehicle, null);
            t.setServerStatus(taskStatus);
            slotMap.put(slot, t);
        }
        when(taskRepository.latestTasksForEachSlot(vehicle, packageType)).thenReturn(slotMap);
        return packages;
    }

}