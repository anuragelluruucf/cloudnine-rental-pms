package dev.stayledger.web;

import dev.stayledger.service.CurrentUserService;
import dev.stayledger.service.GuestMessageService;
import dev.stayledger.web.Dtos.AskRequest;
import dev.stayledger.web.Dtos.DraftRequest;
import dev.stayledger.web.Dtos.MessageResponse;
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
public class MessageController {

    private final GuestMessageService messages;
    private final CurrentUserService currentUser;

    public MessageController(GuestMessageService messages, CurrentUserService currentUser) {
        this.messages = messages;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/messages")
    List<MessageResponse> inbox() {
        return messages.listAll(currentUser.require());
    }

    @GetMapping("/api/properties/{propertyId}/messages")
    List<MessageResponse> forProperty(@PathVariable UUID propertyId) {
        return messages.listForProperty(currentUser.require(), propertyId);
    }

    @PostMapping("/api/properties/{propertyId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    MessageResponse ask(@PathVariable UUID propertyId, @Valid @RequestBody AskRequest request) {
        return messages.ask(currentUser.require(), propertyId, request);
    }

    @PutMapping("/api/messages/{id}/draft")
    MessageResponse draft(@PathVariable UUID id, @Valid @RequestBody DraftRequest request) {
        return messages.updateDraft(currentUser.require(), id, request);
    }

    @PostMapping("/api/messages/{id}/approve")
    MessageResponse approve(@PathVariable UUID id, @Valid @RequestBody DraftRequest request) {
        return messages.approve(currentUser.require(), id, request);
    }
}
