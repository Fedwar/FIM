package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.reports.Report;
import fleetmanagement.backend.reports.ReportService;
import fleetmanagement.backend.reports.vehicles.ConnectionStatusReport;
import fleetmanagement.backend.reports.vehicles.DiagnosisReport;
import fleetmanagement.backend.reports.vehicles.OperationDataReport;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.VehiclesReport;
import fleetmanagement.frontend.webserver.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("report")
@Component
public class Reports extends FrontendController {

    private final VehicleRepository vehicles;
    private final GroupRepository groups;
    private final OperationDataRepository operationDataRepository;
    private final ReportService reportService;

    @Autowired
    public Reports(
            UserSession session,
            VehicleRepository vehicles,
            GroupRepository groups,
            OperationDataRepository operationDataRepository,
            ReportService reportService) {

        super(session);
        this.vehicles = vehicles;
        this.groups = groups;
        this.operationDataRepository = operationDataRepository;
        this.reportService = reportService;
    }

    @GET
    public ModelAndView<Object> showVehiclesReport() {
        return new ModelAndView<>(
                "vehicles-report.html",
                new VehiclesReport(session, groups, vehicles, operationDataRepository)
        );
    }

    @GET
    @Path("diagnosis/{earliestReportDate}/{latestReportDate}/{selectedVehicles}/{rangeBy}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadDiagnosisReport(
            @PathParam("earliestReportDate") String earliestReportDate,
            @PathParam("latestReportDate") String latestReportDate,
            @PathParam("selectedVehicles") String selectedVehicles,
            @PathParam("rangeBy") String rangeBy) {

        Report report = reportService.makeReport(new DiagnosisReport(
                earliestReportDate,
                latestReportDate,
                selectedVehicles,
                rangeBy
        ));
        return getResponse(report);
    }

    @GET
    @Path("operation-data/{earliestReportDate}/{latestReportDate}/{selectedVehicles}/{rangeBy}/{selectedIndicators}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadOperationDataReport(
            @PathParam("earliestReportDate") String earliestReportDate,
            @PathParam("latestReportDate") String latestReportDate,
            @PathParam("selectedVehicles") String selectedVehicles,
            @PathParam("rangeBy") String rangeBy,
            @PathParam("selectedIndicators") String selectedIndicators) {

        Report report = reportService.makeReport(new OperationDataReport(
                earliestReportDate,
                latestReportDate,
                selectedVehicles,
                rangeBy,
                selectedIndicators
        ));
        return getResponse(report);
    }

    @GET
    @Path("connection-status/{earliestReportDate}/{latestReportDate}/{selectedVehicles}/{rangeBy}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadConnectionStatusReport(
            @PathParam("earliestReportDate") String earliestReportDate,
            @PathParam("latestReportDate") String latestReportDate,
            @PathParam("selectedVehicles") String selectedVehicles,
            @PathParam("rangeBy") String rangeBy) {

        Report report = reportService.makeReport(new ConnectionStatusReport(
                earliestReportDate,
                latestReportDate,
                selectedVehicles,
                rangeBy
        ));
        return getResponse(report);
    }

    private Response getResponse(Report report) {
        Response.ResponseBuilder response = Response.ok(report.getBytes());
        response.header("Content-Disposition","attachment; filename=" + report.getFileName());
        return response.build();
    }
}
