package dev.stayledger.web;

import dev.stayledger.service.CurrentUserService;
import dev.stayledger.service.DashboardService;
import dev.stayledger.web.Dtos.DashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboard;
    private final CurrentUserService currentUser;

    public DashboardController(DashboardService dashboard, CurrentUserService currentUser) {
        this.dashboard = dashboard;
        this.currentUser = currentUser;
    }

    @GetMapping
    DashboardResponse dashboard() {
        return dashboard.dashboard(currentUser.require());
    }
}
