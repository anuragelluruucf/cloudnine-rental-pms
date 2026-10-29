package dev.cloudnine.web;

import dev.cloudnine.service.ChannelService;
import dev.cloudnine.service.CurrentUserService;
import dev.cloudnine.service.IcalService;
import dev.cloudnine.web.Dtos.ChannelRequest;
import dev.cloudnine.web.Dtos.ChannelResponse;
import dev.cloudnine.web.Dtos.IcsImportRequest;
import dev.cloudnine.web.Dtos.ImportReport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ChannelController {

    private final ChannelService channels;
    private final IcalService ical;
    private final CurrentUserService currentUser;

    public ChannelController(ChannelService channels, IcalService ical, CurrentUserService currentUser) {
        this.channels = channels;
        this.ical = ical;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/properties/{propertyId}/channels")
    List<ChannelResponse> list(@PathVariable UUID propertyId) {
        return channels.list(currentUser.require(), propertyId);
    }

    @PostMapping("/api/properties/{propertyId}/channels")
    @ResponseStatus(HttpStatus.CREATED)
    ChannelResponse create(@PathVariable UUID propertyId, @Valid @RequestBody ChannelRequest request) {
        return channels.create(currentUser.require(), propertyId, request);
    }

    @PutMapping("/api/channels/{id}")
    ChannelResponse update(@PathVariable UUID id, @Valid @RequestBody ChannelRequest request) {
        return channels.update(currentUser.require(), id, request);
    }

    @PostMapping("/api/channels/{id}/sync")
    ImportReport sync(@PathVariable UUID id) {
        return ical.sync(currentUser.require(), id);
    }

    @PostMapping("/api/channels/{id}/import")
    ImportReport importFeed(@PathVariable UUID id, @Valid @RequestBody IcsImportRequest request) {
        channels.require(currentUser.require(), id);
        return ical.importFeed(id, request.ics());
    }
}
