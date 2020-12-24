package fleetmanagement.frontend.controllers;

import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.security.webserver.GuestAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("language")
@GuestAllowed
@Component
public class Language extends FrontendController {

    @Autowired
    public Language(UserSession request) {
        super(request);
    }

    @POST
    @Path("{language}")
    public Response editGroup(@PathParam("language") String language, String json)  {
        session.setSelectedLanguage(language);
        return Response.status(Response.Status.OK).build();
    }
}
