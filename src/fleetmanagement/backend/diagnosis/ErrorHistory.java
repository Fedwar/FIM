package fleetmanagement.backend.diagnosis;

import fleetmanagement.backend.diagnosis.DeviceSnapshot.StateSnapshot;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
public class ErrorHistory {

    private List<StateEntry> errors = new ArrayList<>();

    public ErrorHistory() {
    }

    public ErrorHistory(List<StateEntry> errors) {
        this.errors.addAll(errors);
    }

    public ErrorHistory clone() {
        return new ErrorHistory(new ArrayList<>(errors));
    }

    public StateEntry getLastEntry() {
        if (errors.size() == 0)
            return null;
        return errors.get(errors.size() - 1);
    }

    public void integrate(StateSnapshot state, ZonedDateTime timestamp) {
        StateEntry newStatus;
        if (state == null)
            newStatus = new StateEntry(timestamp, null, "N/A", null, null);
        else
            newStatus = new StateEntry(timestamp, null, state.code, state.type, state.description);

        StateEntry lastStatus = getLastEntry();
        if (lastStatus == null) {
            errors.add(newStatus);
        } else {
            if (!lastStatus.isStatusEquivalent(newStatus)) {
                errors.remove(errors.size() - 1);
                errors.add(lastStatus.endingAt(timestamp));
                errors.add(newStatus);
            }
        }
    }

    public void integrate(List<StateSnapshot> errorHistory) {
        errors.clear();
        errors = errorHistory.stream()
                .map(StateSnapshot::toStateEntry)
                .collect(Collectors.toList());
    }

    public StateEntry findLatestEqualState(StateEntry stateEntry) {
        if (stateEntry.category != ErrorCategory.ERROR)
            return stateEntry;

        return errors.stream()
                .limit(100)
                .filter(historyEntry -> historyEntry.start != null && historyEntry.category == stateEntry.category)
                .findFirst()
                .orElse(stateEntry);
    }

    public List<StateEntry> getEntries() {
        return Collections.unmodifiableList(errors);
    }

    public void clear() {
        errors = new ArrayList<>();
    }

}
