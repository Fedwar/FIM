package fleetmanagement.frontend.webserver;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;

import fleetmanagement.backend.notifications.NotificationService;
import org.apache.log4j.Logger;

import fleetmanagement.frontend.model.Login;

@Provider
public class FrontendExceptionMapper implements ExceptionMapper<Exception> {
	private static final Logger logger = Logger.getLogger(FrontendExceptionMapper.class);
	private final NotificationService notificationService;

	public FrontendExceptionMapper(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Override
    public Response toResponse(Exception ex) {		
    	int status = 500;
    	ModelAndView<?> webpage;
    	if (ex instanceof WebApplicationException) {
    		WebApplicationException wae = (WebApplicationException)ex;
    	    status = wae.getResponse().getStatus();
    	    webpage = getErrorPage(wae);
    	} else {
    		webpage = getDefaultErrorPage(ex);
    	}
    	
		return Response.status(status).type(MediaType.TEXT_HTML).entity(webpage).build();
    }
		
	private ModelAndView<?> getErrorPage(WebApplicationException ex) {
		switch (ex.getResponse().getStatus()) {
			case 401:
				return new ModelAndView<>("login.html", new Login());
			case 403:
				return new ModelAndView<>("403.html", null);
			case 404:
				return new ModelAndView<>("404.html", null);
			default:
				return getDefaultErrorPage(ex);
		}
	}

	private ModelAndView<?> getDefaultErrorPage(Exception ex) {
		logger.error("Exception while serving webpage: ", ex);
		return new ModelAndView<>("500.html", null);
	}

}