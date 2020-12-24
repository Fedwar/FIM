package fleetmanagement.backend.notifications;

import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Type;
import org.junit.Test;

import javax.mail.internet.AddressException;

import static org.junit.Assert.assertEquals;

public class NotificationSettingTest {

    NotificationSetting tested;
    String defultMail = "mail@mail.com";

    @Test
    public void setMailList() throws AddressException {
        tested = new NotificationSetting(Type.DIAGNOSED_DEVICE_ERROR, defultMail, null);
        String mailList = "mail1@mail.com, mail2@mail.com";
        tested.setMailList(mailList);

        assertEquals(mailList, tested.getMailList());
    }

    @Test(expected = AddressException.class)
    public void mailListInvalidWhenEmpty() throws AddressException {
        tested = new NotificationSetting(Type.DIAGNOSED_DEVICE_ERROR, "", null);
    }

    @Test(expected = AddressException.class)
    public void mailListInvalidSeparator() throws AddressException {
        tested = new NotificationSetting(Type.DIAGNOSED_DEVICE_ERROR, defultMail, null);
        String mailList = "mail1@mail.com; mail2@mail.com";
        tested.setMailList(mailList);

    }

    @Test(expected = AddressException.class)
    public void mailListInvalidAddress() throws AddressException {
        tested = new NotificationSetting(Type.DIAGNOSED_DEVICE_ERROR, defultMail, null);
        String mailList = "mail1mail.com";
        tested.setMailList(mailList);
    }

    @Test(expected = AddressException.class)
    public void mailListInvalidAddress1() throws AddressException {
        tested = new NotificationSetting(Type.DIAGNOSED_DEVICE_ERROR, defultMail, null);
        String mailList = "mail@1mailcom";
        tested.setMailList(mailList);
    }

}