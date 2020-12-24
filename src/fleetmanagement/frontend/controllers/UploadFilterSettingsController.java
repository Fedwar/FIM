package fleetmanagement.frontend.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fleetmanagement.backend.vehiclecommunication.upload.filter.FilterSequenceRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.FilterType;
import fleetmanagement.backend.vehiclecommunication.upload.filter.PathComposer;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilter;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterJson;
import fleetmanagement.backend.vehiclecommunication.upload.filter.UploadFilterSequence;
import fleetmanagement.config.Licence;
import fleetmanagement.config.Settings;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.FilterDirectory;
import fleetmanagement.frontend.model.UploadFilterSettingsModel;
import fleetmanagement.frontend.security.webserver.ConfigRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static fleetmanagement.backend.vehiclecommunication.upload.filter.FilterType.AD_FILTER_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

@Path("/admin/upload-filter")
@Component
public class UploadFilterSettingsController extends FrontendController {

    private static final Logger logger = Logger.getLogger(UploadFilterSettingsController.class);

    @Autowired
    private Settings settings;
    private final FilterSequenceRepository filterSequenceRepository;
    private final Licence licence;

    @Autowired
    public UploadFilterSettingsController(UserSession session, FilterSequenceRepository filterSequenceRepository,
                                          Licence licence) {
        super(session);
        this.filterSequenceRepository = filterSequenceRepository;
        this.licence = licence;
    }

    @GET
    @ConfigRoleRequired
    public ModelAndView<UploadFilterSettingsModel> getUploadFilterSequence() {
        UploadFilterSequence filterSequence = filterSequenceRepository.findByType(AD_FILTER_TYPE);
        logger.debug("Loading filters. Found: " + filterSequence.filters.size());

        UploadFilterSettingsModel vm = new UploadFilterSettingsModel(filterSequence, licence);

        vm.filterEditMemo = i18n("ad_filter_add_modal_comment");
        vm.filtersUseMemo = i18n("ad_filter_use_memo");
        vm.patternExamples = i18n("ad_filter_condition_pattern_example");

        return new ModelAndView<>("admin-upload-filter.html", vm);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @ConfigRoleRequired
    public Response saveUploadFilter(String data) {
        Type listType = new TypeToken<ArrayList<UploadFilterJson>>() {
        }.getType();
        List<UploadFilterJson> filterList = new Gson().fromJson(data, listType);

        try {
            filterSequenceRepository.updateOrInsert(
                    fs -> {
                        fs.filters.clear();
                        for (UploadFilterJson uploadFilter : filterList) {
                            fs.addFilter(uploadFilter.toFilter());
                        }
                    }
            );
        } catch (Error e) {
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n("ad_filter_save_error", e.getMessage()))
                    .build();
        }

        return Response.status(Response.Status.OK)
                .entity(i18n("ad_filter_save_success"))
                .build();
    }

    @GET
    @Path("dir>{directory: .*}")
    @ConfigRoleRequired
    public ModelAndView<FilterDirectory> viewFilterDirectory(@PathParam("directory") String directory) {
        UploadFilterSequence filterSequence = filterSequenceRepository.findByType(FilterType.AD_FILTER_TYPE);
        UploadFilter filter = filterSequence.getFilterByDirectory(directory);

        FilterDirectory vm = new FilterDirectory(filter, directory, licence, settings);

        return new ModelAndView<>("admin-directory-browser.html", vm);
    }

    @GET
    @Path("root>{directory: .*}")
    @ConfigRoleRequired
    public ModelAndView<FilterDirectory> viewFilterRootDirectory(@PathParam("directory") String directory) {
        String cleanPath = PathComposer.getCleanPath(directory);
        UploadFilterSequence filterSequence = filterSequenceRepository.findByType(FilterType.AD_FILTER_TYPE);
        UploadFilter filter = filterSequence.getFilterByDirectory(cleanPath);

        FilterDirectory vm = new FilterDirectory(filter, cleanPath, licence, settings);

        return new ModelAndView<>("admin-directory-browser.html", vm);
    }


    @GET
    @Path("file>{path: .*}")
    @ConfigRoleRequired
    public Response downloadFile(@PathParam("path") String path) throws FileNotFoundException, UnsupportedEncodingException {
        UploadFilterSequence filterSequence = filterSequenceRepository.findByType(FilterType.AD_FILTER_TYPE);
        UploadFilter filter = filterSequence.getFilterByDirectory(path);

        if (filter != null) {
            File loadFile = new File(path);

            File filterDir = new File(PathComposer.getCleanPath(filter));
            if (!filterDir.isAbsolute()) {
                String incomingFolderPath = settings.getIncomingFolderPath();
                loadFile = new File(incomingFolderPath, path);
            }

            Response response;
            if (loadFile.exists()) {
                String filename = loadFile.getName().replace(" ", "_");
                response = Response.ok().type(APPLICATION_OCTET_STREAM)
                        .header("content-disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, "UTF-8")).
                                entity(new FileInputStream(loadFile)).build();
            } else
                response = Response.noContent().build();

            filterSequenceRepository.update(
                    filterSequence.id,
                    fs -> fs.notViewedFiles.remove(
                            path.replace('/',
                                    File.separatorChar).replace('\\', File.separatorChar)
                    )
            );

            return response;
        } else {
            return Response.noContent().build();
        }
    }

}
