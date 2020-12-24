package fleetmanagement.backend.operationData;

import com.google.gson.internal.LinkedTreeMap;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OperationDataSnapshot{
	public ZonedDateTime created;
	public List<Indicator> indicators = new ArrayList<>();



	public OperationDataSnapshot() {
	}

	public OperationDataSnapshot(LinkedTreeMap<String, LinkedTreeMap<String, Object>> linkedTreeMap) {
		created = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS);

		if (linkedTreeMap.size() > 0) {
			for (Map.Entry<String, LinkedTreeMap<String, Object>> entry : linkedTreeMap.entrySet()) {
				Object unit = entry.getValue().get("unit");
				Object value = entry.getValue().get("value");
				if (unit == null)
					unit = "";
				if(!(unit instanceof String)) {
					//throw new Exception("unit is not String");
				}
				indicators.add(new Indicator(entry.getKey(), unit.toString(), value, created));
			}
		}
	}

}
