package fleetmanagement.backend.operationData;

import java.time.ZonedDateTime;
import java.util.Objects;

public class History {
    public final Object value;
    public final ZonedDateTime timeStamp;

    public History(Object value, ZonedDateTime timeStamp) {
        this.value = value;
        this.timeStamp = timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        History history = (History) o;
        return Objects.equals(value, history.value) &&
                Objects.equals(timeStamp, history.timeStamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, timeStamp);
    }

    public void value() {
    }
}
