package fleetmanagement.backend.operationData;

import fleetmanagement.backend.repositories.Persistable;
import gsp.util.WrappedException;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OperationData implements Persistable<UUID> {
	public final UUID vehicleId;
	public ZonedDateTime updated;
	public final List<Indicator> indicators;

	public OperationData(UUID vehicleId, ZonedDateTime updated, List<Indicator> indicators) {
		this.vehicleId = vehicleId;
		this.updated = updated;
		this.indicators = indicators;
	}

	public OperationData(UUID vehicleId) {
		this.vehicleId = vehicleId;
		this.updated = ZonedDateTime.now();
		this.indicators = new ArrayList();
	}

	public List<Indicator> setIndicatorValue(String id, Object value, String unit) {
		Indicator indicator = indicators.stream().filter(x -> x.id.equals(id)).findFirst().orElse(null);
		if (indicator == null ) {
			indicators.add(new Indicator(id, unit, value, updated));
		} else {
			indicator.setValue(value, updated);
		}
		return indicators;
	}

	public Indicator getIndicator(String id) {
		return indicators.stream().filter(x -> x.id.equals(id)).findFirst().orElse(null);
	}

	@Override
	public UUID id() {
		return vehicleId;
	}

	@Override
	public OperationData clone() {
		try {
			return (OperationData)super.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new WrappedException(e);
		}
	}
}
