package fleetmanagement.backend.vehiclecommunication;

import fleetmanagement.backend.repositories.exception.VehicleCountExceeded;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import org.apache.log4j.Logger;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.time.ZonedDateTime;

@Path("/login")
public class LoginResource {
	
	private final VehicleRepository repository;
	private final int protocolNumber;
	private static final Logger logger = Logger.getLogger(LoginResource.class);
	
	public LoginResource(VehicleRepository repository, int protocolNumber) {
		this.repository = repository;
		this.protocolNumber = protocolNumber;
	}

	@POST
	public Response login(@FormParam("uic") String uic, @FormParam("version") String version, @FormParam("additional_uic") String additional_uic ) {
		if (uic == null || uic.isEmpty() || version == null || version.isEmpty())
			throw new WebApplicationException(Status.BAD_REQUEST);

		try {
			Vehicle vehicle = new Vehicle(uic, additional_uic, uic, version, ZonedDateTime.now(), null, true, protocolNumber);
			repository.insertOrUpdate(vehicle, v -> {
				v.lastSeen = ZonedDateTime.now();
				v.lastSeenProtocol = protocolNumber;
				v.clientVersion = version;
				v.additional_uic = additional_uic;
			});
		} catch (VehicleCountExceeded e ) {
		    logger.warn(e.getMessage());
		    return Response.status(Status.FORBIDDEN).build();
		}
        return Response.status(Status.OK).build();
	}
}
