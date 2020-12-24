package fleetmanagement.frontend.controllers;

import fleetmanagement.config.Licence;
import fleetmanagement.frontend.Frontend;
import fleetmanagement.frontend.model.DataMigrationModel;
import fleetmanagement.frontend.security.webserver.AdminRoleRequired;
import fleetmanagement.frontend.security.webserver.UserRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.usecases.DataMigration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.rythmengine.utils.S.i18n;

@Path("migration")
@Component
public class DataMigrationController {

    final DataMigration dataMigration;
    private Licence licence;

    @Autowired
    public DataMigrationController(DataMigration dataMigration, Licence licence) {
        this.dataMigration = dataMigration;
        this.licence = licence;
    }

    @GET
    @UserRoleRequired
    public ModelAndView<DataMigrationModel> getUploadPackageUI() {
        return new ModelAndView<>("data-migration.html", new DataMigrationModel(licence));
    }


    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path("import")
    @AdminRoleRequired
    public Response viewFilterDirectory(String sourcePath) {
        try {
            dataMigration.importData(sourcePath);
            dataMigration.importLicence(sourcePath);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(i18n("migration_import_error", e.getMessage()))
                    .build();
        }

        return Response.status(Response.Status.OK)
                .entity(i18n("migration_import_success"))
                .build();
    }
}
