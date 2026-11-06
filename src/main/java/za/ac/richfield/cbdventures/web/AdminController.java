package za.ac.richfield.cbdventures.web;

import org.springframework.web.bind.annotation.*;
import za.ac.richfield.cbdventures.domain.User;
import za.ac.richfield.cbdventures.domain.enums.BusinessStatus;
import za.ac.richfield.cbdventures.security.CurrentUser;
import za.ac.richfield.cbdventures.service.AdminService;
import za.ac.richfield.cbdventures.web.dto.AdminDtos;
import za.ac.richfield.cbdventures.web.dto.BusinessCard;

import java.util.List;

/**
 * Administrator verification queue (Phase 3: publish only after verification).
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/businesses")
    public List<BusinessCard> businesses(@RequestParam(defaultValue = "PENDING") String status) {
        return adminService.byStatus(BusinessStatus.valueOf(status.toUpperCase()));
    }

    @PostMapping("/businesses/{id}/approve")
    public BusinessCard approve(@PathVariable Long id,
                                @RequestBody(required = false) AdminDtos.DecisionRequest request) {
        User admin = CurrentUser.require();
        return adminService.decide(admin, id, BusinessStatus.ACTIVE,
                request == null ? null : request.note());
    }

    @PostMapping("/businesses/{id}/reject")
    public BusinessCard reject(@PathVariable Long id,
                               @RequestBody(required = false) AdminDtos.DecisionRequest request) {
        User admin = CurrentUser.require();
        return adminService.decide(admin, id, BusinessStatus.REJECTED,
                request == null ? null : request.note());
    }

    @GetMapping("/stats")
    public AdminDtos.StatsView stats() {
        return adminService.stats();
    }
}
