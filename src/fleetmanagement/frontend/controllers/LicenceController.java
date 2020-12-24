package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.LicenceModel;
import fleetmanagement.frontend.security.webserver.AdminRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.util.Locale;

@Path("/licence")
@Component
public class LicenceController extends FrontendController {

    private static final Logger logger = Logger.getLogger(LicenceController.class);

    private final Licence licence;
    private final VehicleRepository vehicleRepository;

    @Autowired
    public LicenceController(UserSession session, Licence licence, VehicleRepository vehicleRepository) {
        super(session);
        this.licence = licence;
        this.vehicleRepository = vehicleRepository;
    }

    @GET
    @AdminRoleRequired
    public ModelAndView<LicenceModel> getLicenceInfo() {
        return new ModelAndView<>("licence-status.html",
                new LicenceModel(licence, vehicleRepository));
    }

    @POST
    @Path("/addon")
    @AdminRoleRequired
    public Response addon(@FormParam("encrypted-command") String command) {
        logger.debug(command);
        if (licence.update(command)) {
            licence.saveLicenceToFile(command);

            if (!licence.getLanguages().isEmpty()) {
                if (!licence.getLanguages().contains(session.getSelectedLanguage())) {
                    if (licence.getLanguages().contains(Locale.getDefault().getLanguage()))
                        session.setSelectedLanguage(Locale.getDefault().getLanguage());
                    else
                        session.setSelectedLanguage(licence.getLanguages().get(0));
                }
            }
        }
        return Redirect.to("/licence");

    }

}
