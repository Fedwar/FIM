package fleetmanagement.frontend.controllers;

import java.io.InputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.WebFiles;
import fleetmanagement.frontend.security.webserver.GuestAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("css")
@GuestAllowed
@Component
public class Css implements FrontendResource {

	@GET
	@Path("/{filename:[A-z0-9/\\.\\-]+\\.css}")
	@Produces("text/css")
	public InputStream getCssFile(@PathParam("filename") String filename) {
		return WebFiles.open("css/" + filename);
	}
	
	@GET
	@Path("/{filename:.*\\.(ttf|eot|woff|woff2|svg|png)}")
	@Produces("application/octet-stream")
	public Response getFontFile(@PathParam("filename") String filename) {
		CacheControl cc = new CacheControl();
		cc.setMaxAge(3600);
		cc.setPrivate(true);		
		return Response.ok(WebFiles.open("css/" + filename), MediaType.APPLICATION_OCTET_STREAM).cacheControl(cc).build();
	}
}
