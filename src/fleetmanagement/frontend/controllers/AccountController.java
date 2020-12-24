package fleetmanagement.frontend.controllers;

import com.google.gson.Gson;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import fleetmanagement.backend.accounts.Account;
import fleetmanagement.backend.accounts.AccountRepository;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.languages.Languages;
import fleetmanagement.frontend.model.AccountModel;
import fleetmanagement.frontend.webserver.ModelAndView;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URLConnection;

@Path("account")
@Component
public class AccountController extends FrontendController {

    private final AccountRepository accountRepository;
    private final Languages languages;
    private static final Logger logger = Logger.getLogger(Notifications.class);

    @Autowired
    public AccountController(UserSession session, AccountRepository accountRepository, Languages languages) {
        super(session);
        this.accountRepository = accountRepository;
        this.languages = languages;
    }

    @GET
    public ModelAndView<AccountModel> getAccount() {
        Account account = accountRepository.findByLogin(session.getUsername());
        if (account == null)
            return new ModelAndView<>("404.html", null);
        return new ModelAndView<>("account.html", new AccountModel(account, languages.getLanguages()));
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("photo")
    public Response upload(
            @FormDataParam("photo") InputStream data,
            @FormDataParam("photo") FormDataContentDisposition meta,
            @FormDataParam("photo-name") String fileName) {
        String filename = (fileName != null && !fileName.isEmpty()) ? fileName : meta.getFileName();
        filename = extractFilename(filename);
        try {
            accountRepository.setAccountPhoto(session.getUsername(), filename, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }

    @POST
     public Response update(String data) {
        try {
            Account accountJson = new Gson().fromJson(data, Account.class);
            Account account = accountRepository.findByLogin(session.getUsername());
            accountRepository.update(account.id, a -> {
                a.phone = accountJson.phone;
                a.name = accountJson.name;
                a.email = accountJson.email;
                a.address = accountJson.address;
                a.website = accountJson.website;
                a.twitter = accountJson.twitter;
            });
        } catch (Exception e) {
            logger.error("Json parsing error", e);
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n("account_save_error"))
                    .build();
        }

        return Response.ok().build();
    }

    @GET
    @Path("photo")
    public Response getImgFile() throws IOException {
        File file = accountRepository.getAccountPhoto(session.getUsername());
        if (file == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        String mime = URLConnection.guessContentTypeFromName(file.getName());
        return Response.ok(new FileInputStream(file)).type(mime).build();
    }

    @DELETE
    @Path("photo")
    public Response deletePhoto() throws IOException {
        accountRepository.setAccountPhoto(session.getUsername(), null, null);
        return Response.ok().build();
    }

    private String extractFilename(String filename) {
        int lastSlash = StringUtils.lastIndexOfAny(filename, "/", "\\");
        if (lastSlash == -1)
            return filename;

        return filename.substring(lastSlash + 1);
    }

    @POST
    @Path("language/{language}")
    public Response switchLanguage(@PathParam("language") String language, String json)  {
        Account profile = accountRepository.findByLogin(session.getUsername());
        accountRepository.update(profile.id, p -> {
            p.language = language;
        });
        session.setSelectedLanguage(language);
        return Response.status(Response.Status.OK).build();
    }
}
