package fleetmanagement.backend.reports.datasource.vehicles;

import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import org.apache.log4j.Logger;

import java.util.*;

public class OperationDataReportDataSource extends VehiclesReportDataSource {
    private static final Logger logger = Logger.getLogger(DiagnosisReportDataSource.class);
    private OperationDataRepository operationData;
    private List<String> selectedIndicators;
    //Map<vehicleName, Map<indicatorName, Map<date, value>>>
    private ArrayList<VehicleData> vehicleDatas;

    public OperationDataReportDataSource(
            OperationDataRepository operationData,
            VehicleRepository vehicles,
            Map<String, String> filters) {

        super(vehicles, filters);
        this.operationData = operationData;
        this.selectedIndicators = Arrays.asList(filters.get("selectedIndicators").split(","));
        generateData();
    }

    public ArrayList<VehicleData> getData() {
        return vehicleDatas;
    }

    @Override
    void generateData() {
        vehicleDatas = new ArrayList<>();
        for (String vehicleId : selectedVehicles) {
            VehicleData vehicleData = new VehicleData(vehicles.tryFindById(UUID.fromString(vehicleId)).getName());
            vehicleDatas.add(vehicleData);
            for (String indicatorId : selectedIndicators) {
                IndicatorData indicatorData = new IndicatorData(indicatorId);
                vehicleData.indicators.add(indicatorData);
                List<History> histories = operationData.getIndicatorHistory(UUID.fromString(vehicleId), indicatorId);
                for (History historyItem : histories) {
                    String date = VehiclesReportDataSourceUtils.toReportFormatDate(historyItem.timeStamp, rangeBy);
                    if (VehiclesReportDataSourceUtils.isDateInRange(
                            historyItem.timeStamp,
                            earliestReportDate,
                            latestReportDate
                    ))
                        try {
                            indicatorData.history.put(date, Double.parseDouble(historyItem.value.toString()));
                        } catch (NumberFormatException e) {
                            logger.warn("Wrong number format of history item");
                        }
                }
                indicatorData.history = VehiclesReportDataSourceUtils.getSortedMapByKey(indicatorData.history);
            }
        }
    }

    public static class VehicleData {
        public final String vehicleName;
        public final List<IndicatorData> indicators;

        VehicleData(String vehicleName) {
            this.vehicleName = vehicleName;
            this.indicators = new ArrayList<>();
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VehicleData that = (VehicleData) o;
            return vehicleName.equals(that.vehicleName) &&
                    indicators.equals(that.indicators);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vehicleName, indicators);
        }
    }

    public static class IndicatorData {
        public final String indicatorId;
        public Map<String, Double> history;

        IndicatorData(String indicatorId) {
            this.indicatorId = indicatorId;
            history = new HashMap<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IndicatorData that = (IndicatorData) o;
            return indicatorId.equals(that.indicatorId) &&
                    history.equals(that.history);
        }

        @Override
        public int hashCode() {
            return Objects.hash(indicatorId, history);
        }
    }
}