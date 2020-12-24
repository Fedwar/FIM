package fleetmanagement.backend.diagnosis;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class StateEntry implements Cloneable {
    public final ZonedDateTime start;
    public final ZonedDateTime end;
    public final String code;
    public final ErrorCategory category;
    public final LocalizedString message;

    public StateEntry(ZonedDateTime start, ZonedDateTime end, String code, ErrorCategory category, LocalizedString message) {
        if (start != null )
            start = start.truncatedTo(ChronoUnit.MILLIS);
        if (end != null )
            end = end.truncatedTo(ChronoUnit.MILLIS);
        this.start = start;
        this.end = end;
        this.code = code;
        this.category = category;
        this.message = (message == null ? new LocalizedString() : message);
    }

    public boolean isOk() {
        return category == ErrorCategory.OK;
    }

    public boolean isStatusEquivalent(StateEntry other) {
        if (other == null)
            return false;
        return Objects.equals(other.code, code) && other.category == category && Objects.equals(other.message, message);
    }

    public StateEntry endingAt(ZonedDateTime endedAt) {
        return new StateEntry(start, endedAt, code, category, message);
    }

    public StateEntry startingAt(ZonedDateTime startedAt) {
        return new StateEntry(startedAt, end, code, category, message);
    }

    @Override
    protected StateEntry clone(){
        try {
            return (StateEntry)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
