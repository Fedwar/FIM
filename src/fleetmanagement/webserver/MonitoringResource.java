package fleetmanagement.webserver;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import fleetmanagement.frontend.controllers.Redirect;
import fleetmanagement.frontend.security.webserver.GuestAllowed;

@Path("monitoring")
@GuestAllowed
public class MonitoringResource {
	
	@GET
	public Response monitoringLandingPage() {
		return Redirect.to("/monitoring/health");
	}

	@GET
	@Path("health")
	public String reportServerHealth() {
		return "200 OK";
	}
	
	@Path("health")
	@GuestAllowed
	public static class HealthRedirectResource
	{
		@GET
		public Response redirectToHealthResource() {
			return Redirect.to("/monitoring/health");
		}
	}
}
