package za.ac.richfield.cbdventures.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Reporting views. Every report is returned as a generic table (columns + rows)
 * so the web client can render any of them with one component and the CSV
 * export can be produced from the same object.
 */
public final class ReportDtos {

    private ReportDtos() {
    }

    /** Headline number shown above the table. */
    public record Metric(String label, String value) {
    }

    /** One row of a report. Cells are pre-formatted strings for display/CSV. */
    public record ReportView(
            String key,
            String title,
            String description,
            String generatedAt,
            List<String> columns,
            List<List<String>> rows,
            List<Metric> metrics
    ) {
    }

    /** Entry in the report picker. */
    public record ReportInfo(String key, String title, String description) {
    }
}
