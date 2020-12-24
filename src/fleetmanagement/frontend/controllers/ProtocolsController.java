package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.Backend;
import fleetmanagement.backend.repositories.disk.OnDiskVehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.ProtocolList;
import fleetmanagement.frontend.security.webserver.UserRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;

import com.sun.jersey.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("https")
@Component
public class ProtocolsController extends FrontendController {
    private final Backend backend;
    private Licence licence;
    private OnDiskVehicleRepository vehicleRepository;

    @Autowired
    public ProtocolsController(UserSession session, Backend backend, Licence licence, OnDiskVehicleRepository vehicleRepository) {
        super(session);
        this.backend = backend;
        this.licence = licence;
        this.vehicleRepository = vehicleRepository;
    }

    @GET
    @UserRoleRequired
    public ModelAndView<ProtocolList> getProtocolsList() {
        return new ModelAndView<>("protocol-list.html",
            new ProtocolList(backend, licence, vehicleRepository));
    }

    @GET
    @Path("{number}")
    @UserRoleRequired
    public ModelAndView<ProtocolList.ProtocolItemDetail> protocolDetail(@PathParam("number") int number) {
		if (number < 0 || number > 3)
            throw new WebApplicationException(Status.NOT_FOUND);

        return new ModelAndView<>("protocol-item.html",
			new ProtocolList.ProtocolItemDetail(number, backend, licence, vehicleRepository));
    } 
    
	@POST
    @Path("{number}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @UserRoleRequired
    public ModelAndView<ProtocolList.ProtocolItemDetail> update(@PathParam("number") int number, 
			@FormDataParam("enable") String enable, @FormDataParam("disable") String disable,
            @FormDataParam("key") InputStream keyStream, @FormDataParam("trust") InputStream trustStream, @FormDataParam("cert") String saveCerts) {
		
		if (number < 0 || number > 3)
            throw new WebApplicationException(Status.NOT_FOUND);
		
		int errno = 0;

		if (enable != null) {
			try {
				backend.enableProtocol(number);
			} catch (Exception ignored) {
			}

		} else if (disable != null) {
			backend.disableProtocol(number);
		} else if (saveCerts != null) {
			if (backend.storeCert(number, keyStream, trustStream))
				errno = 1;
			else
				errno = 2;
		}

		ProtocolList.ProtocolItemDetail model = new ProtocolList.ProtocolItemDetail(number, backend, licence, vehicleRepository);
		model.errno = errno;

        return new ModelAndView<>("protocol-item.html", model);
    }
}
