package fleetmanagement.frontend.controllers;

import java.net.*;

import javax.ws.rs.core.Response;

import gsp.util.WrappedException;

public class Redirect {

	public static Response to(String url) {
		try {
			return Response.seeOther(new URI(url)).build();
		} catch (URISyntaxException e) {
			throw new WrappedException(e);
		}
	}

}
