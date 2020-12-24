package fleetmanagement.test;

import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.vehicles.Vehicle;

public class TestScenarioPrefilled extends TestScenario {
	public Vehicle vehicle1;
	public Vehicle vehicle2;
	public Vehicle vehicle3;
	public Diagnosis diagnosis1;
	public LicenceStub licence;
	public Package package1;
	public Package package2;
	public Package package3;
	public Package package4;
	public Package package5;
	public Group group1;
	public Group group2;

	public TestScenarioPrefilled() {

        vehicle1 = addVehicle();
		vehicle2 = addVehicle();
		vehicle3 = addVehicle();

		diagnosis1 = addDiagnosis(vehicle1);

		licence = new LicenceStub();

		package1 = addPackage(PackageType.DataSupply, "1.0", 1, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		package2 = addPackage(PackageType.DataSupply, "1.1", 2, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		package3 = addPackage(PackageType.DataSupply, "1.2", 1, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		package4 = addPackage(PackageType.CopyStick, "1.0", null, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		package5 = addPackage(PackageType.CopyStick, "2.0", null, "08.09.2013 00:00:00", "01.12.2013 23:59:59");

		group1 = addGroup("group1","group1",true);
		group2 = addGroup("group2","group2",true);
	}
}
