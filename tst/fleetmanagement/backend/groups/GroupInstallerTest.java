package fleetmanagement.backend.groups;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.usecases.DeletePackage;
import fleetmanagement.usecases.InstallPackage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static fleetmanagement.TestObjectFactory.createPackage;
import static fleetmanagement.TestObjectFactory.createVehicle;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GroupInstallerTest {
    
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private PackageRepository packageRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private PackageTypeRepository packageTypeRepository;
    @Mock
    private InstallPackage installPackage;
    @Mock
    private DeletePackage deletePackage;

    private GroupInstaller tested;

    private Package pkg = createPackage(PackageType.DataSupply, "1.0", 1, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
    private Package duplicate = createPackage(PackageType.DataSupply, "1.0", 1, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
    private Group group = new Group("group1","group1",true);
    private Vehicle vehicle = createVehicle("vehicle1");

    @Before
    public void setup() {
        when(installPackage.startInstallation(any(), anyList(), isNull())).thenReturn(new InstallPackage.StartInstallationResult());
        tested = new GroupInstaller(vehicleRepository, packageRepository
                , taskRepository, packageTypeRepository, installPackage, deletePackage);
    }

    private void installOnGroup() {
        when(packageRepository.duplicate(pkg, group)).thenReturn(duplicate);
        when(vehicleRepository.listByGroup(group.id.toString())).thenReturn(asList(vehicle));

        tested.assignPackageToGroup(pkg, group);
    }

    @Test
    public void assignPackageToGroup_CreatesDuplicateOfPackage() {
        installOnGroup();

        verify(packageRepository).duplicate(pkg, group);

        verify(packageRepository, never()).update(eq(duplicate.id), any());
        verify(packageRepository, never()).update(eq(pkg.id), any());

        verify(installPackage).startInstallation(duplicate, Arrays.asList(vehicle), null);
    }

    @Test
    public void assignPackageToGroup_DoNothing_IfPackageOrItsDuplicateAssignedToThisGroup() {
        when(packageRepository.isGroupContainsPackageDuplicate(pkg, group)).thenReturn(true);

        installOnGroup();

        verify(packageRepository).isGroupContainsPackageDuplicate(pkg, group);
        verifyNoMoreInteractions(packageRepository);
    }

    @Test
    public void removePackageFromGroup_DeletesDuplicateAssignedToGroup() {
        when(packageRepository.getDuplicates(pkg)).thenReturn(asList(pkg, duplicate));
        when(packageRepository.listByGroupId(group.id)).thenReturn(singletonList(duplicate));

        tested.removePackageFromGroup(pkg, group, null);

        verify(deletePackage).deleteById(duplicate.id, null);
    }

    @Test
    public void removePackageFromGroup_notDeleteLastPackage() {
        when(packageRepository.getDuplicates(pkg)).thenReturn(asList(pkg));
        pkg.groupId = group.id;

        tested.removePackageFromGroup(pkg, group, null);

        verifyZeroInteractions(deletePackage);
        assertThat(pkg.groupId, nullValue());
    }

    @Test
    public void assignVehicles_startsInstallation() {
        when(packageRepository.listByGroupId(group.id)).thenReturn(singletonList(pkg));

        tested.assignVehicles(group, singletonList(vehicle));

        verify(installPackage).startInstallation(pkg, Arrays.asList(vehicle), null);
    }

    @Test
    public void removesAllPackagesByGroupId() {
        when(packageRepository.listByGroupId(group.id)).thenReturn(asList(pkg, duplicate));
        when(packageRepository.getDuplicates(pkg)).thenReturn(asList(pkg, duplicate));
        when(packageRepository.getDuplicates(duplicate)).thenReturn(asList(duplicate));

        tested.removeAllPackagesByGroupId(group.id);

        verify(packageRepository, times(1)).delete(any());
        verify(packageRepository).delete(pkg.id);
    }
}