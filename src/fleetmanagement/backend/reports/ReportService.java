package fleetmanagement.backend.reports;

import fleetmanagement.backend.reports.datasource.ReportDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportService {
    @Autowired
    private ReportProvider reportProvider;

    public ReportService() {
    }

    ReportService(ReportProvider reportProvider) {
        this.reportProvider = reportProvider;
    }

    public Report makeReport(Report report) {
        ReportDataSource reportDataSource = reportProvider.getReportDataSource(report);
        report.build(reportDataSource);
        return report;
    }
}