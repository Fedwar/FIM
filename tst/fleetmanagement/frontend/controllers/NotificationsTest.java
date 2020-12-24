package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.mail.MailService;
import fleetmanagement.backend.notifications.DiagnosedDeviceError;
import fleetmanagement.backend.notifications.TestNotification;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Type;
import fleetmanagement.frontend.model.NotificationsModel;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.ws.rs.core.Response;

import static fleetmanagement.backend.notifications.settings.Type.DIAGNOSED_DEVICE_ERROR;
import static fleetmanagement.backend.notifications.settings.Type.DIAGNOSIS_MAX_ERRORS;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.rythmengine.utils.S.i18n;

public class NotificationsTest {

    private TestScenarioPrefilled scenario;
    private Notifications tested;
    String mailList = "dev@gsp.com";
    @Mock
    MailService mailService;

    @Before
    public void setup() throws AddressException {
        MockitoAnnotations.initMocks(this);
        scenario = new TestScenarioPrefilled();
        tested = new Notifications(new SessionStub(), scenario.licence, scenario.notificationRepository
                , scenario.diagnosisRepository, scenario.operationDataRepository, scenario.vehicleRepository
                , scenario.groupRepository, mailService, scenario.accountRepository);
    }

    @Test
    public void showNotifications() throws AddressException {
        addNotification(DIAGNOSED_DEVICE_ERROR, mailList);
        addNotification(DIAGNOSIS_MAX_ERRORS, mailList);

        ModelAndView<NotificationsModel> model = tested.showNotifications();
        assertEquals("admin-notifications.html", model.page);
        assertEquals(2, model.viewmodel.notifications.size());
        assertEquals(DIAGNOSED_DEVICE_ERROR, model.viewmodel.notifications.get(0).type);
        assertEquals(DIAGNOSIS_MAX_ERRORS, model.viewmodel.notifications.get(1).type);
    }

    @Test
    public void deleteNotification() throws AddressException {
        addNotification(DIAGNOSED_DEVICE_ERROR, mailList);
        NotificationSetting notificationSetting = addNotification(DIAGNOSIS_MAX_ERRORS, mailList);

        assertEquals(2, scenario.notificationRepository.listAll().size());
        tested.deleteNotification(notificationSetting.id.toString());
        assertEquals(1, scenario.notificationRepository.listAll().size());
    }

    @Test
    public void saveNotification() {
        Response response = tested.saveNotification(getJsonString(mailList));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(1, scenario.notificationRepository.findByType(DIAGNOSIS_MAX_ERRORS).size());
        NotificationSetting notificationSetting = scenario.notificationRepository.findByType(DIAGNOSIS_MAX_ERRORS).get(0);
        assertEquals(mailList, notificationSetting.getMailList());
        assertEquals(1, notificationSetting.getParameters().size());

    }

    @Test
    public void invalidMail() {
        String invalidMail = "invalid.com";

        Response response = tested.saveNotification(getJsonString(invalidMail));

        assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
        assertEquals(i18n("notif_invalid_email"), response.getEntity());
        assertEquals(0, scenario.notificationRepository.findByType(DIAGNOSIS_MAX_ERRORS).size());
    }

    private NotificationSetting addNotification(Type type, String mailList) throws AddressException {
        NotificationSetting notificationSetting = null;
        switch (type) {
            case DIAGNOSED_DEVICE_ERROR:
                notificationSetting = NotificationSetting.diagnosedDeviceError("device", mailList);
                break;
            case DIAGNOSIS_MAX_ERRORS:
                notificationSetting = NotificationSetting.diagnosisMaxErrors(5, mailList);
                break;
            case INDICATOR_VALUE_RANGE:
                notificationSetting = NotificationSetting.indicatorValueRange("indicator", 0, 10, mailList);
                break;
        }
        scenario.notificationRepository.insert(notificationSetting);
        return notificationSetting;
    }

    private String getJsonString(String mailList) {
        return "{\"id\":\"\",\"type\":\"DIAGNOSIS_MAX_ERRORS\",\"mailList\":\"" + mailList + "\"," +
                "\"parameters\":[[\"ERROR_LIMIT\",\"2\"]],\"byMail\":true,\"byLog\":true}";
    }

    @Test
    public void sendTestNotification() throws MessagingException {
        Response response = tested.sendTestNotification(getJsonString(mailList));
        verify(mailService).sendThrowingException(any(TestNotification.class));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void responcesError_IfMailIncorrect() {
        String incorrectMail = "devgspcom";
        Response response = tested.sendTestNotification(getJsonString(incorrectMail));
        verify(mailService, never()).send(any(TestNotification.class));
        assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), response.getStatus());
    }

    @Test
    public void responsesError_IfMessagingException() throws MessagingException {
        doThrow(MessagingException.class)
                .when(mailService)
        .sendThrowingException(any(TestNotification.class));
        Response response = tested.sendTestNotification(getJsonString(mailList));
        assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
    }
}