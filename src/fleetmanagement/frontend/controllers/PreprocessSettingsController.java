package fleetmanagement.frontend.controllers;

import com.google.gson.Gson;
import com.sun.jersey.api.NotFoundException;
import fleetmanagement.backend.packages.preprocess.PreprocessSetting;
import fleetmanagement.backend.packages.preprocess.PreprocessSettingRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.PreprosessSettingsModel;
import fleetmanagement.frontend.security.webserver.UserRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

import static fleetmanagement.frontend.model.PreprosessSettingsModel.*;

@Path("/preprocess-settings")
@Component
public class PreprocessSettingsController extends FrontendController {

    private static final Logger logger = Logger.getLogger(PreprocessSettingsController.class);
    private final Licence licence;
    private final PreprocessSettingRepository settings;

    @Autowired
    public PreprocessSettingsController(UserSession session, Licence licence, PreprocessSettingRepository settings) {
        super(session);
        this.licence = licence;
        this.settings = settings;
    }

    @GET
    @UserRoleRequired
    public ModelAndView<PreprosessSettingsModel> show() {
        return new ModelAndView("admin-preprocessing.html", new PreprosessSettingsModel(session, licence, settings));
    }

    @DELETE
    @Path("{id}")
    @UserRoleRequired
    public Response delete(@PathParam("id") String id) {
        PreprocessSetting setting = settings.tryFindById(UUID.fromString(id));
        if (setting == null) {
            logger.warn("Can't delete notificationSetting which doesn't exist! PreprocessSetting ID = " + id);
        } else {
            settings.delete(UUID.fromString(id));
        }
        return Response.ok().build();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @UserRoleRequired
    public Response save(String data) {
        SettingModel settingJson;
        try {
            settingJson = new Gson().fromJson(data, SettingModel.class);
        } catch (Exception e) {
            logger.error("Json parsing error", e);
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n("preprocess_setting_save_error"))
                    .build();
        }

        try {
            PreprocessSetting setting = settingJson.toSetting();
            validate(setting);
            settings.insertOrReplace(setting);
        } catch (InvalidParameter invalidParameter) {
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n(invalidParameter.getMessage()))
                    .build();
        }
        return Response.status(Response.Status.OK)
                .entity("success")
                .build();
    }

    private void validate(PreprocessSetting setting) throws InvalidParameter {

    }

    public class InvalidParameter extends Exception {
        public InvalidParameter(String message) {
            super(message);
        }
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public SettingModel get(@PathParam("id") String settingId) {
        UUID uuid = UUID.fromString(settingId);
        PreprocessSetting setting = settings.tryFindById(uuid);
        if (setting == null)
            throw new NotFoundException();

        return new SettingModel(setting, session);
    }


}
