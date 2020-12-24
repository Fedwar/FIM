package fleetmanagement.backend.reports.vehicles;

import fleetmanagement.backend.reports.Report;
import org.apache.log4j.Logger;
import org.apache.poi.xddf.usermodel.PresetColor;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public abstract class VehiclesReport implements Report {

    private static final int bufferInitialSize = 10000;
    private static final Logger logger = Logger.getLogger(VehiclesReport.class);

    Map<String, String> filters = new HashMap<>();
    private String fileName;
    ByteArrayOutputStream outputStream;

    VehiclesReport(
            String earliestReportDate,
            String latestReportDate,
            String selectedVehicles,
            String rangeBy,
            String fileName) {

        filters.put("earliestReportDate", earliestReportDate);
        filters.put("latestReportDate", latestReportDate);
        filters.put("selectedVehicles", selectedVehicles);
        filters.put("rangeBy", rangeBy);
        this.fileName = fileName;
    }

    @Override
    public Map<String, String> getFilters() {
        return filters;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public byte[] getBytes() {
        return outputStream == null ? new byte[0] : outputStream.toByteArray();
    }

    static String generateDateInFileNameFormat() {
        return ZonedDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );
    }

    void writeToExcel(XSSFWorkbook workbook) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bufferInitialSize)) {
            workbook.write(outputStream);
            this.outputStream = outputStream;
        } catch (IOException e) {
            logger.warn("Unable to save report to excel", e);
        }
    }

    static void solidFillSeries(XDDFChartData data, int index, PresetColor color) {
        XDDFSolidFillProperties fill = new XDDFSolidFillProperties(XDDFColor.from(color));
        XDDFChartData.Series series = data.getSeries().get(index);
        XDDFShapeProperties properties = series.getShapeProperties();
        if (properties == null) {
            properties = new XDDFShapeProperties();
        }
        properties.setFillProperties(fill);
        series.setShapeProperties(properties);
    }
}
