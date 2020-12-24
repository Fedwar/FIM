package fleetmanagement.backend.reports.vehicles;

import fleetmanagement.backend.reports.ReportType;
import fleetmanagement.backend.reports.datasource.ReportDataSource;
import fleetmanagement.backend.reports.datasource.vehicles.ConnectionStatusReportDataSource;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFChartLegend;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConnectionStatusReport extends VehiclesReport {
    private XSSFWorkbook workbook = new XSSFWorkbook();
    private XSSFSheet sheet = workbook.createSheet("Connection status report");

    public ConnectionStatusReport(
            String earliestReportDate,
            String latestReportDate,
            String selectedVehicles,
            String rangeBy) {
        super(earliestReportDate, latestReportDate, selectedVehicles, rangeBy,
                "ConnectionReport_" + generateDateInFileNameFormat() + ".xlsx");
    }

    @Override
    public ReportType getReportType() {
        return ReportType.CONNECTION_STATUS;
    }

    @Override
    public void build(ReportDataSource reportDataSource) {
        Map<String, ConnectionStatusReportDataSource.DataItem> data =
                ((ConnectionStatusReportDataSource)reportDataSource).getData();
        writeRowToSheet(
                sheet,
                0,
                Arrays.asList("Date", "All vehicles count", "Online", "Irregular", "Offline")
        );
        int rowNumber = 1;
        for (ConnectionStatusReportDataSource.DataItem dataItem : data.values()) {
            int sum = dataItem.onlineCount + dataItem.irregularCount + dataItem.offlineCount;
            writeRowToSheet(
                    sheet,
                    rowNumber,
                    Arrays.asList(
                            dataItem.date,
                            sum,
                            dataItem.onlineCount,
                            dataItem.irregularCount,
                            dataItem.offlineCount
                    )
            );
            rowNumber++;
        }

        writeChartToSheet(sheet, data.size());

        writeToExcel(workbook);
    }

    private void writeRowToSheet(XSSFSheet sheet, int startRowNumber, List<?> cells) {
        XSSFRow row = sheet.createRow(startRowNumber);
        int colIndex = 0;
        for (Object value : cells) {
            XSSFCell cell = row.createCell(colIndex);
            if (value.getClass().equals(String.class))
                cell.setCellValue(value.toString());
            else
                cell.setCellValue(Double.parseDouble(value.toString()));
            colIndex++;
        }
    }

    private void writeChartToSheet(XSSFSheet sheet, int dataSize) {
        //Making chart's size and title
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(
                0, 0, 0, 0,
                0,
                dataSize + 3,
                23,
                dataSize + 26
        );
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText("Connection statuses");
        chart.setTitleOverlay(false);
        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.TOP_RIGHT);

        //Making axises
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Dates");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);
        leftAxis.setTitle("Vehicles count");

        //Making sources
        XDDFDataSource<String> datesSource = XDDFDataSourcesFactory.fromStringCellRange(
                sheet,
                new CellRangeAddress(1, dataSize, 0, 0)
        );
        XDDFNumericalDataSource<Double> onlineSource = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet,
                new CellRangeAddress(1, dataSize, 2, 2)
        );
        XDDFNumericalDataSource<Double> irregularSource = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet,
                new CellRangeAddress(1, dataSize, 3, 3)
        );
        XDDFNumericalDataSource<Double> offlineSource = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet,
                new CellRangeAddress(1, dataSize, 4, 4)
        );

        //Making plot
        XDDFChartData data = chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
        XDDFChartData.Series onlineSeries = data.addSeries(datesSource, onlineSource);
        onlineSeries.setTitle("Online", null);
        XDDFChartData.Series irregularSeries = data.addSeries(datesSource, irregularSource);
        irregularSeries.setTitle("Irregular", null);
        XDDFChartData.Series offlineSeries = data.addSeries(datesSource, offlineSource);
        offlineSeries.setTitle("Offline", null);
        chart.plot(data);
    }
}
