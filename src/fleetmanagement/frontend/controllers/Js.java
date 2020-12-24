package fleetmanagement.frontend.controllers;

import java.io.*;

import javax.ws.rs.*;

import org.apache.commons.io.input.ReaderInputStream;

import fleetmanagement.frontend.*;
import fleetmanagement.frontend.security.webserver.GuestAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("js")
@GuestAllowed
@Component
public class Js extends FrontendController {

	@Autowired
	public Js(UserSession session) {
		super(session);
	}

	@GET
	@Path("/{filename:[A-z0-9\\-/\\.]+}")
	@Produces("text/javascript")
	public InputStream getJsFile(@PathParam("filename") String filename) {
		if (mightContainRythmScripts(filename)) {
			StringReader reader = new StringReader(Templates.renderJavascript(filename, session.getLocale(), session.getSecurityContext(), session.getUsername()));
			return new ReaderInputStream(reader);
		} else {
			return WebFiles.open("js/" + filename);
		}		
	}

	private boolean mightContainRythmScripts(String filename) {
		return filename.endsWith(".rythm.js");
	}
}