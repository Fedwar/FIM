package fleetmanagement.backend.operationData;

import gsp.util.WrappedException;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class Indicator implements Cloneable {

	public final String id;
	public final String unit;
	public Object value;
	public ZonedDateTime updated;
	@Deprecated
	private List<History> history;

	public Indicator(String id, String unit, Object value, ZonedDateTime updated, List<History> history) {
		this.id = id;
		this.unit = unit;
		this.value = value;
		this.updated = updated;
		this.history = new ArrayList<>(emptyIfNull(history));
	}

	public Indicator(String id, String unit, Object value, ZonedDateTime updated) {
		this.id = id;
		this.unit = unit;
		this.history = new ArrayList<>();
		setValue(value, updated);
	}

	public Indicator(String id, String unit, Object value) {
		this(id, unit, value, ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS));
	}

	public void setValue(Object value, ZonedDateTime updated) {
		this.value = value;
		if (updated != null)
			updated = updated.truncatedTo(ChronoUnit.MILLIS);
		this.updated = updated;
	}

	//history migrated to OperationDataHistoryRepository
	@Deprecated
	public List<History> getHistory() {
		return history;
	}

	@Deprecated
	public void clearHistory() {
		this.history.clear();
	}

	@Override
	public Indicator clone() {
		try {
			return (Indicator)super.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new WrappedException(e);
		}
	}
}
