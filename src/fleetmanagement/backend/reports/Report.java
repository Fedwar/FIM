package fleetmanagement.backend.reports;

import fleetmanagement.backend.reports.datasource.ReportDataSource;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

public interface Report {
    ReportType getReportType();
    String getFileName();
    byte[] getBytes();
    Map<String, String> getFilters();
    void build(ReportDataSource reportDataSource);
}
