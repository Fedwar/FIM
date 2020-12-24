package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.frontend.model.OperationDataModel;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

public class OperationDataControllerTest {
    private TestScenarioPrefilled scenario;
    private OperationDataController tested;
    private Vehicle vehicle;

    @Before
    public void setup() {
        scenario = new TestScenarioPrefilled();
        vehicle = scenario.vehicle1;

        tested = new OperationDataController(new SessionStub(), scenario.vehicleRepository
                , scenario.operationDataRepository, scenario.widgetRepository, scenario.licence);
    }

    @Test
    public void showOperationData() {
        OperationData operationData = addOperationData(vehicle.id);
        ModelAndView<OperationDataModel> model = tested.showOperationData(vehicle.id.toString());

        assertEquals("operation-data.html", model.page);
        assertEquals(vehicle.id.toString(), model.viewmodel.vehicleId);
        assertEquals(vehicle.getName(), model.viewmodel.vehicleName);
        assertEquals(operationData.indicators.size(), model.viewmodel.indicators.size());
    }

    private OperationData addOperationData(UUID vehicleId) {
        return scenario.addOperationData(vehicleId, new Indicator("tank1", "liter", "12") );
    }


}