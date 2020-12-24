package fleetmanagement.frontend.controllers;

import java.io.InputStream;
import java.net.URLConnection;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import fleetmanagement.frontend.Frontend;
import fleetmanagement.frontend.WebFiles;
import fleetmanagement.frontend.security.webserver.GuestAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("img")
@GuestAllowed
@Component
public class Img implements FrontendResource {

	@GET
	@Path("/{filename}")
	public Response getImgFile(@PathParam("filename") String filename) {
		String mime = URLConnection.guessContentTypeFromName(filename);
		InputStream file = WebFiles.open("img/" + filename);
		return Response.ok(file).type(mime).build();
	}
}
