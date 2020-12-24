package fleetmanagement.frontend.security.webserver;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface UserRoleRequired {

}
