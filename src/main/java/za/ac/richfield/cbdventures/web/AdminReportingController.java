package za.ac.richfield.cbdventures.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.security.CurrentUser;
import za.ac.richfield.cbdventures.service.BackupService;
import za.ac.richfield.cbdventures.service.ReportService;
import za.ac.richfield.cbdventures.web.dto.BackupDtos;
import za.ac.richfield.cbdventures.web.dto.ReportDtos;

import java.util.List;

/**
 * Reporting and system backup endpoints (administrator only).
 *
 * <p>Both features are restricted by {@code SecurityConfig}
 * ({@code /api/admin/**} requires ROLE_ADMIN) and re-checked inside the services.</p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminReportingController {

    private final ReportService reportService;
    private final BackupService backupService;

    public AdminReportingController(ReportService reportService, BackupService backupService) {
        this.reportService = reportService;
        this.backupService = backupService;
    }

    /* ------------------------------------------------------------ reports */

    /** Catalogue of the five management reports. */
    @GetMapping("/reports")
    public List<ReportDtos.ReportInfo> reports() {
        return reportService.catalogue();
    }

    /** One report as JSON (table shaped) for the on-screen view. */
    @GetMapping("/reports/{key}")
    public ReportDtos.ReportView report(@PathVariable String key) {
        return reportService.run(key);
    }

    /** The same report as a downloadable CSV file (opens in Excel). */
    @GetMapping("/reports/{key}/csv")
    public ResponseEntity<String> reportCsv(@PathVariable String key) {
        ReportDtos.ReportView report = reportService.run(key);
        String fileName = "cbdventures-" + report.key() + "-"
                + report.generatedAt().replaceAll("[^0-9]", "") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(reportService.toCsv(report));
    }

    /* ------------------------------------------------------------- backup */

    /** Full logical backup as a JSON download. */
    @GetMapping("/backup")
    public ResponseEntity<BackupDtos.BackupFile> backup() {
        User admin = CurrentUser.require();
        BackupDtos.BackupFile file = backupService.export(admin);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + backupService.suggestedFileName() + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(file);
    }

    /** Summary of what a backup would contain (used by the screen before download). */
    @GetMapping("/backup/summary")
    public BackupDtos.BackupCounts backupSummary() {
        User admin = CurrentUser.require();
        return backupService.export(admin).counts();
    }

    /**
     * Restores a backup document uploaded from the administrator screen.
     * The raw request body is read directly so the file is accepted exactly as
     * the browser downloaded it (no re-serialisation).
     */
    @PostMapping("/backup/restore")
    public BackupDtos.RestoreResult restore(HttpServletRequest request) throws IOException {
        User admin = CurrentUser.require();
        String body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return backupService.restore(admin, body);
    }
}
