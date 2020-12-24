package fleetmanagement.backend.webserver;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class UnknownVehicleRequest extends WebApplicationException {
    public UnknownVehicleRequest() {
    }

    public UnknownVehicleRequest(Response response) {
        super(response);
    }

    public UnknownVehicleRequest(int status) {
        super(status);
    }

    public UnknownVehicleRequest(Response.Status status) {
        super(status);
    }

    public UnknownVehicleRequest(Throwable cause) {
        super(cause);
    }

    public UnknownVehicleRequest(Throwable cause, Response response) {
        super(cause, response);
    }

    public UnknownVehicleRequest(Throwable cause, int status) {
        super(cause, status);
    }

    public UnknownVehicleRequest(Throwable cause, Response.Status status) {
        super(cause, status);
    }
}
