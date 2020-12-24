package fleetmanagement.frontend.controllers;

import com.google.gson.Gson;
import fleetmanagement.backend.accounts.AccountRepository;
import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.mail.MailService;
import fleetmanagement.backend.notifications.TestNotification;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.NotificationSettingRepository;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.notifications.settings.Type;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.NotificationsModel;
import fleetmanagement.frontend.security.webserver.UserRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/admin/notifications")
@Component
public class Notifications extends FrontendController {

    private static final Logger logger = Logger.getLogger(Notifications.class);
    private final Licence licence;
    private final NotificationSettingRepository notificationSettingRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final OperationDataRepository operationDataRepository;
    private final VehicleRepository vehicleRepository;
    private final GroupRepository groupRepository;
    private final MailService mailService;
    private final AccountRepository accountRepository;

    @Autowired
    public Notifications(UserSession session, Licence licence
            , NotificationSettingRepository notificationSettingRepository, DiagnosisRepository diagnosisRepository
            , OperationDataRepository operationDataRepository, VehicleRepository vehicleRepository
            , GroupRepository groupRepository, MailService mailService, AccountRepository accountRepository) {
        super(session);
        this.licence = licence;
        this.notificationSettingRepository = notificationSettingRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.operationDataRepository = operationDataRepository;
        this.vehicleRepository = vehicleRepository;
        this.groupRepository = groupRepository;
        this.mailService = mailService;
        this.accountRepository = accountRepository;
    }

    @GET
    @UserRoleRequired
    public ModelAndView<NotificationsModel> showNotifications() {
        return new ModelAndView("admin-notifications.html", new NotificationsModel(session, licence,
                notificationSettingRepository, diagnosisRepository, operationDataRepository, vehicleRepository
                , groupRepository, accountRepository));
    }

    @DELETE
    @Path("{id}")
    @UserRoleRequired
    public Response deleteNotification(@PathParam("id") String id) {
        NotificationSetting notificationSetting = notificationSettingRepository.tryFindById(UUID.fromString(id));
        if (notificationSetting == null) {
            logger.warn("Can't delete notificationSetting which doesn't exist! NotificationSetting ID = " + id);
        } else {
            notificationSettingRepository.delete(UUID.fromString(id));
        }
        return Redirect.to("/admin/notifications");
    }

    @POST
    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    @UserRoleRequired
    public Response sendTestNotification(String data) {
        NotificationJson notificationJson;
        try {
            notificationJson = new Gson().fromJson(data, NotificationJson.class);
        } catch (Exception e) {
            logger.error("Json parsing error", e);
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n("notif_save_error"))
                    .build();
        }


        try {
            NotificationSetting notificationSetting = notificationJson.toNotification();
            TestNotification testNotification = new TestNotification(notificationSetting);
            mailService.sendThrowingException(testNotification);
        } catch (AddressException e) {
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n("notif_invalid_email"))
                    .build();
        } catch (MessagingException e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(e.getLocalizedMessage())
                    .build();
        }
        return Response.status(Response.Status.OK)
                .entity(i18n("notif_mail_sent"))
                .build();
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @UserRoleRequired
    public Response saveNotification(String data) {
        NotificationJson notificationJson;
        try {
            notificationJson = new Gson().fromJson(data, NotificationJson.class);
        } catch (Exception e) {
            logger.error("Json parsing error", e);
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n("notif_save_error"))
                    .build();
        }


        try {
            NotificationSetting notificationSetting = notificationJson.toNotification();
            validate(notificationSetting);
            notificationSettingRepository.insertOrReplace(notificationSetting);
        } catch (AddressException e) {
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n("notif_invalid_email"))
                    .build();
        } catch (InvalidParameter invalidParameter) {
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n(invalidParameter.getMessage()))
                    .build();
        }
        return Response.status(Response.Status.OK)
                .entity("success")
                .build();
    }

    private void validate(NotificationSetting notificationSetting) throws InvalidParameter {
        if (notificationSetting.type == Type.VEHICLE_OFFLINE) {
            String timeout = notificationSetting.getParameter(Parameter.VEHICLE_OFFLINE_TIMEOUT);
            if (timeout.isEmpty() || timeout.equals("0")) {
                throw new InvalidParameter("notif_invalid_offline_timeout");
            }
        }
    }

    public class InvalidParameter extends Exception {
        public InvalidParameter(String message) {
            super(message);
        }
    }

    @DoNotObfuscate
    public static class NotificationJson {
        public String id;
        public String type;
        public String mailList;
        public Map<String, String> parameters;

        public NotificationSetting toNotification() throws AddressException {
            UUID uuid = null;
            if (id == null || id.isEmpty())
                uuid = UUID.randomUUID();
            else
                uuid = UUID.fromString(id);

            return new NotificationSetting(uuid, Type.valueOf(type), mailList,
                    parameters.entrySet().stream()
                            .collect(Collectors.toMap(e -> Parameter.valueOf(e.getKey()), e -> e.getValue()))
            );
        }
    }


}
