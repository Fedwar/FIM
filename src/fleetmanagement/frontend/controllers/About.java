package fleetmanagement.frontend.controllers;

import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.webserver.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;

@Path("/about")
@Component
public class About extends FrontendController {

    @Autowired
    public About(UserSession session) {
        super(session);
    }

    @GET
    public ModelAndView<fleetmanagement.frontend.model.About> getAboutUI() {
        fleetmanagement.frontend.model.About vm = new fleetmanagement.frontend.model.About();
        return new ModelAndView<>("about.html", vm);
    }
}