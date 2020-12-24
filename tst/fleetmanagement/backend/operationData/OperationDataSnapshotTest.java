package fleetmanagement.backend.operationData;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OperationDataSnapshotTest {

    private final Gson gson = new Gson();

    @Test
    public void indicatorsTimestampIsSameAsSnapshot() {
        String json = "{'dieseltank1': {     'value': '2330',     'unit': 'liter'   }" +
                ",'dieseltank2': {     'value': 65,     'unit': 'liter'   }" +
                ",'lowFuel': {     'value': true   }}";

        OperationDataSnapshot snapshot = new OperationDataSnapshot(gson.fromJson(json, LinkedTreeMap.class));
        assertEquals(snapshot.created, snapshot.indicators.get(0).updated);
        assertEquals(snapshot.created, snapshot.indicators.get(1).updated);
        assertEquals(snapshot.created, snapshot.indicators.get(2).updated);
    }

}