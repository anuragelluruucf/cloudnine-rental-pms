package dev.cloudnine.web;

import dev.cloudnine.service.CurrentUserService;
import dev.cloudnine.service.DashboardService;
import dev.cloudnine.web.Dtos.DashboardResponse;
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
