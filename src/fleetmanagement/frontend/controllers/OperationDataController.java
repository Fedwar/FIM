package fleetmanagement.frontend.controllers;

import com.sun.jersey.api.NotFoundException;
import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.backend.widgets.Widget;
import fleetmanagement.backend.widgets.WidgetRepository;
import fleetmanagement.backend.widgets.WidgetType;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.*;
import fleetmanagement.frontend.webserver.ModelAndView;
import gsp.util.DoNotObfuscate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Path("operation-data")
@Component
public class OperationDataController extends FrontendController {
    private final VehicleRepository vehicles;
    private final OperationDataRepository operationDataRepository;

    private final WidgetRepository widgetRepository;
    private final Licence licence;

    @Autowired
    public OperationDataController(UserSession session, VehicleRepository vehicles
            , OperationDataRepository operationDataRepository, WidgetRepository widgetRepository, Licence licence) {
        super(session);
        this.vehicles = vehicles;
        this.operationDataRepository = operationDataRepository;
        this.widgetRepository = widgetRepository;

        this.licence = licence;
    }

    @GET
    @Path("/{id}/ajax")
    public ModelAndView<Object> operationDataAjax(@PathParam("id") String id) {
        UUID vehicleId = UUID.fromString(id);

        Vehicle vehicle = vehicles.tryFindById(vehicleId);
        if (vehicle == null)
            throw new NotFoundException();

        return new ModelAndView<>("operation-data-template.html", new OperationDataModel(vehicle, operationDataRepository, licence));
    }


    @GET
    @Path("/{id}")
    public ModelAndView<OperationDataModel> showOperationData(@PathParam("id") String id) {
        //chart conversion library requests this file here for unknown reason, so we just skip it.
        if (id != null && id.equals("weblysleekuil.ttf"))
            return null;

        UUID vehicleId = UUID.fromString(id);

        Vehicle vehicle = vehicles.tryFindById(vehicleId);
        if (vehicle == null)
            throw new NotFoundException();

        return new ModelAndView<>("operation-data.html", new OperationDataModel(vehicle, operationDataRepository, licence));
    }

    @GET
    @Path("/{id}/indicator/{indicatorId}")
    public ModelAndView<IndicatorHistory> showOperationDataIndicatorHistory(
            @PathParam("id") String id, @PathParam("indicatorId") String indicatorId) {
        UUID vehicleId = UUID.fromString(id);
        OperationData operationData = operationDataRepository.tryFindById(vehicleId);

        Vehicle vehicle = vehicles.tryFindById(vehicleId);
        if (vehicle == null)
            throw new NotFoundException();

        Indicator indicator = null;

        if (operationData != null)
            indicator = operationData.getIndicator(indicatorId);

        if (indicator == null)
            throw new NotFoundException();

        IndicatorHistory indicatorHistory = new IndicatorHistory(vehicle, indicator,
                operationDataRepository.getIndicatorHistory(vehicle.id, indicator.id));

        return new ModelAndView<>("indicator-history.html", indicatorHistory);
    }

    @GET
    @Path("/widget/ajax")
    @Produces(MediaType.APPLICATION_JSON)
    public WidgetData getWidgetSettings(@QueryParam("indicatorId") String indicatorId) {
        Widget widget = widgetRepository.findWidgetByIndicatorId(indicatorId);

        return new WidgetData(null, widget);
    }

    @POST
    @Path("/widget/ajax")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveWidgetSetting(WidgetData widgetData) {
        if (widgetData.type == null) {
            widgetRepository.deleteWidgetByIndicatorId(widgetData.indicatorId);
        } else {
            Widget widget = new Widget(widgetData.indicatorId, widgetData.maxValue, widgetData.minValue, widgetData.type);
            widgetRepository.insertOrReplace(widget);
        }
        return Response.ok().build();
    }

    @GET
    @Path("/widget-data/ajax")
    @Produces(MediaType.APPLICATION_JSON)
    public WidgetData getWidgetData(@QueryParam("vehicleId") String vehicleId, @QueryParam("indicatorId") String indicatorId) {
        UUID vehicleUuid = UUID.fromString(vehicleId);
        OperationData operationData = operationDataRepository.tryFindById(vehicleUuid);
        if (operationData == null)
            throw new NotFoundException();

        Vehicle vehicle = vehicles.tryFindById(vehicleUuid);
        if (vehicle == null)
            throw new NotFoundException();

        Indicator indicator = operationData.getIndicator(indicatorId);
        if (indicator == null)
            throw new NotFoundException();

        Widget widget = widgetRepository.findWidgetByIndicatorId(indicator.id);

        return new WidgetData(indicator, widget, vehicle);
    }

    @DoNotObfuscate
    public class WidgetData {
        Object[][] columns;
        String dateFormat;
        String tickFormat;
        WidgetType type;
        String unit;
        Object maxValue;
        Object minValue;
        String indicatorId;

        public WidgetData() {
        }

        public WidgetData(Indicator indicator, Widget widget) {
            this(indicator, widget, null);
        }

        public WidgetData(Indicator indicator, Widget widget, Vehicle vehicle) {
            if (widget != null) {
                maxValue = widget.maxValue;
                minValue = widget.minValue;
                type = widget.type;

                if (indicator != null) {
                    if (widget.type == WidgetType.CHART) {
                        ZonedDateTime now = ZonedDateTime.now();
                        List<History> indicatorHistory = operationDataRepository
                                .getHistory(vehicle.id, indicator.id
                                        , 20);

                        this.dateFormat = "%Y-%m-%dT%H:%M:%S.%L";
                        this.tickFormat = "%Y-%m-%d %H:%M";
                        String[] xAxis = new String[indicatorHistory.size() + 1];
                        String[] data = new String[indicatorHistory.size() + 1];
                        xAxis[0] = "x";
                        data[0] = indicator.id + "(" + indicator.unit + ")";

                        for (int i = 0; i < indicatorHistory.size(); i++) {
                            History history = indicatorHistory.get(i);
                            xAxis[i + 1] = history.timeStamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                            data[i + 1] = history.value.toString();
                        }
                        columns = new String[][]{xAxis, data};
                    } else {
                        unit = indicator.unit;
                        Object[] data = {indicator.id, indicator.value};
                        columns = new Object[][]{data};
                    }
                }
            }
        }

        public WidgetData(Indicator indicator, List<History> indicatorHistory, String dateFormat, String tickFormat) {
            this.dateFormat = dateFormat;
            this.tickFormat = tickFormat;
            String[] xAxis = new String[indicatorHistory.size() + 1];
            String[] data = new String[indicatorHistory.size() + 1];
            xAxis[0] = "x";
            if (indicator != null)
                data[0] = indicator.id + "(" + indicator.unit + ")";

            for (int i = 0; i < indicatorHistory.size(); i++) {
                History history = indicatorHistory.get(i);
                xAxis[i + 1] = history.timeStamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                data[i + 1] = history.value.toString();
            }

            columns = new String[][]{xAxis, data};
        }

    }

}