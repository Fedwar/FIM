package fleetmanagement.frontend.controllers;

import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;

import fleetmanagement.frontend.Frontend;
import fleetmanagement.frontend.WebFiles;
import fleetmanagement.frontend.security.webserver.GuestAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("/favicon.ico")
@GuestAllowed
@Component
public class Favicon implements FrontendResource {
	@GET
	@Produces("image/x-icon")
	public InputStream getFavicon() {
		return WebFiles.open("img/favicon.ico");
	}
}
