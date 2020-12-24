package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;


import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.GroupList;
import fleetmanagement.frontend.model.MapAndGroups;
import fleetmanagement.frontend.model.MapViewModel;
import fleetmanagement.frontend.webserver.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;

@Path("/map")
@Component
public class MapView extends FrontendController {

    private final VehicleRepository vehicles;
    private final GroupRepository groups;
    private final Licence licence;

    @Autowired
    public MapView(UserSession session, VehicleRepository vehicles, GroupRepository groups, Licence licence) {
        super(session);
        this.vehicles = vehicles;
        this.groups = groups;
        this.licence = licence;
    }

    String getPage() {
        if (licence.isMapAvailable())
            return "map.html";
        else
            return "map-not-licenced.html";
    }

    @GET
    @Produces(MediaType.TEXT_HTML + ";charset=UTF-8")
    public ModelAndView<MapAndGroups> getMapUI() {
        return new ModelAndView<>(getPage(),
                new MapAndGroups(
                        new MapViewModel(vehicles, session),
                        new GroupList(groups.listAll(), null, vehicles)
                )
        );

    }

    @GET
    @Path("ajax")
    @Produces(MediaType.TEXT_HTML + ";charset=UTF-8")
    public ModelAndView<MapAndGroups> getMarkers() {
        ModelAndView<MapAndGroups> mapUI = getMapUI();
        return new ModelAndView<>("map-template.html", mapUI.viewmodel) ;
    }

    @GET
    @Path("group/{groupId}")
    public ModelAndView<MapAndGroups> byGroup(@PathParam("groupId") String groupId) {
        if (groupId == null)
            throw new IllegalArgumentException("Invalid argument of group/{groupId}. groupId cannot be null.");

        return new ModelAndView<>(getPage(),
                new MapAndGroups(
                        new MapViewModel(vehicles, groupId, session),
                        new GroupList(groups.listAll(), groupId, vehicles)
                )
        );
    }

    @GET
    @Path("group/{groupId}/ajax")
    public ModelAndView<MapAndGroups> byGroupAjax(@PathParam("groupId") String groupId) {
        ModelAndView<MapAndGroups> mapUI = byGroup(groupId);
        return new ModelAndView<>("map-template.html", mapUI.viewmodel) ;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML + ";charset=UTF-8")
    public ModelAndView<MapAndGroups> getMapCenteredOnVehicle(@PathParam("id") String id) {
        Vehicle vehicle = vehicles.tryFindById(UUID.fromString(id));

        if (vehicle == null)
            throw new WebApplicationException(Status.NOT_FOUND);

        return new ModelAndView<>(getPage(),
                new MapAndGroups(
                        new MapViewModel(vehicles, vehicle, session),
                        new GroupList(groups.listAll(), null, vehicles)
                )
        );

    }

    @GET
    @Path("{id}/ajax")
    public ModelAndView<MapAndGroups> getMapCenteredOnVehicleAjax(@PathParam("id") String id) {
        ModelAndView<MapAndGroups> mapUI =  getMapCenteredOnVehicle(id);
        return new ModelAndView<>("map-template.html", mapUI.viewmodel) ;
    }

}
