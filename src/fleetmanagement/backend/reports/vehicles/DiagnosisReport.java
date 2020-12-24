package fleetmanagement.backend.reports.vehicles;

import fleetmanagement.backend.reports.ReportType;
import fleetmanagement.backend.reports.datasource.ReportDataSource;
import fleetmanagement.backend.reports.datasource.vehicles.DiagnosisReportDataSource;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;

import java.util.Map;

public class DiagnosisReport extends VehiclesReport {
    private XSSFWorkbook workbook = new XSSFWorkbook();
    private XSSFSheet sheet = workbook.createSheet("Diagnostic report");

    public DiagnosisReport(
            String earliestReportDate,
            String latestReportDate,
            String selectedVehicles,
            String rangeBy) {
        super(earliestReportDate, latestReportDate, selectedVehicles, rangeBy,
                "DiagnosticReport_" + generateDateInFileNameFormat() + ".xlsx");
    }

    @Override
    public ReportType getReportType() {
        return ReportType.DIAGNOSIS;
    }

    @Override
    public void build(ReportDataSource reportDataSource) {
        Map<String, Map<String, Integer>> data = ((DiagnosisReportDataSource)reportDataSource).getData();
        int rowNumber = 0;
        for (String vehicleName : data.keySet()) {
            Map<String, Integer> sortedErrorsCount = data.get(vehicleName);
            writeVehicleDataToSheet(sheet, sortedErrorsCount, rowNumber, vehicleName);
            writeChartToSheet(
                    sheet,
                    sortedErrorsCount.size(),
                    "Errors count per " + filters.get("rangeBy"),
                    rowNumber + 1
            );
            rowNumber += 35;
        }

        writeToExcel(workbook);
    }

    private void writeVehicleDataToSheet(
            XSSFSheet sheet,
            Map<String, ?> data,
            int startRowNumber,
            String vehicleName) {

        XSSFRow vehicleNameRow = sheet.createRow(startRowNumber);
        XSSFRow datesRow = sheet.createRow(startRowNumber + 1);
        XSSFRow valuesRow = sheet.createRow(startRowNumber + 2);
        vehicleNameRow.createCell(0).setCellValue(vehicleName);
        datesRow.createCell(0).setCellValue("Dates");
        valuesRow.createCell(0).setCellValue("Errors count");
        int colIndex = 1;
        for (String date : data.keySet()) {
            XSSFCell dateCell = datesRow.createCell(colIndex);
            XSSFCell valuesCell = valuesRow.createCell(colIndex);
            dateCell.setCellValue(date);
            if (data.get(date).getClass().equals(String.class))
                valuesCell.setCellValue(data.get(date).toString());
            else
                valuesCell.setCellValue(Double.parseDouble(data.get(date).toString()));
            colIndex++;
        }
    }

    private void writeChartToSheet(XSSFSheet sheet, int dataSize, String title, int startRowNumber) {
        //Making chart's size and title
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = drawing.createAnchor(
                0, 0, 0, 0, 0, startRowNumber + 3, 23, startRowNumber + 29
        );
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);
        //XDDFChartLegend legend = chart.getOrAddLegend();
        //legend.setPosition(LegendPosition.TOP_RIGHT);

        //Making axises
        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Dates");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Errors count");
        leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        //Making sources
        XDDFDataSource<String> xs = XDDFDataSourcesFactory.fromStringCellRange(
                sheet,
                new CellRangeAddress(startRowNumber, startRowNumber, 1, dataSize)
        );
        XDDFNumericalDataSource<Double> ys = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet,
                new CellRangeAddress(startRowNumber + 1, startRowNumber + 1, 1, dataSize)
        );

        //Making plot
        XDDFChartData data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        XDDFChartData.Series series = data.addSeries(xs, ys);
        series.setTitle(title, null);
        chart.plot(data);
        XDDFBarChartData bar = (XDDFBarChartData) data;
        bar.setBarDirection(BarDirection.COL);
    }
}
