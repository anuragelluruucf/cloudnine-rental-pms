package dev.stayledger.web;

import dev.stayledger.service.CurrentUserService;
import dev.stayledger.service.IcalService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class CalendarFeedController {

    private static final MediaType CALENDAR = MediaType.parseMediaType("text/calendar");

    private final IcalService ical;
    private final CurrentUserService currentUser;

    public CalendarFeedController(IcalService ical, CurrentUserService currentUser) {
        this.ical = ical;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/properties/{id}/calendar.ics")
    ResponseEntity<String> propertyFeed(@PathVariable UUID id) {
        return calendar(ical.export(currentUser.require(), id), "cloudnine-" + id + ".ics");
    }

    @GetMapping("/api/ical/{token}/calendar.ics")
    ResponseEntity<String> publicFeed(@PathVariable UUID token) {
        return calendar(ical.exportByToken(token), "cloudnine.ics");
    }

    private static ResponseEntity<String> calendar(String body, String filename) {
        return ResponseEntity.ok()
                .contentType(CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(body);
    }
}
