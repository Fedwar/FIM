package fleetmanagement.backend.webserver;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.*;

import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.NotificationService;
import org.apache.log4j.Logger;

@Provider
public class BackendExceptionMapper implements ExceptionMapper<Exception> {
	private static final Logger logger = Logger.getLogger(BackendExceptionMapper.class);
	private final NotificationService notificationService;

	public BackendExceptionMapper(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Override
    public Response toResponse(Exception ex) {		
    	int status = 500;
		if (ex instanceof UnknownVehicleRequest) {
			logger.warn("Web request from unknown vehicle");
			UnknownVehicleRequest wae = (UnknownVehicleRequest)ex;
			status = wae.getResponse().getStatus();
		} else if (ex instanceof WebApplicationException) {
			logger.error("Exception while serving webpage: ", ex);
			WebApplicationException wae = (WebApplicationException)ex;
			status = wae.getResponse().getStatus();
			notificationService.processEvent(Events.serverException(ex));
		} else {
			logger.error("Exception while serving webpage: ", ex);
			notificationService.processEvent(Events.serverException(ex));
		}

		return Response.status(status).build();
    }
}