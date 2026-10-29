package dev.cloudnine.service;

import dev.cloudnine.domain.GuestMessage;
import dev.cloudnine.domain.MessageStatus;
import dev.cloudnine.domain.Property;
import dev.cloudnine.domain.UserAccount;
import dev.cloudnine.repo.GuestMessageRepository;
import dev.cloudnine.repo.PropertyRepository;
import dev.cloudnine.web.ApiException;
import dev.cloudnine.web.Dtos.AskRequest;
import dev.cloudnine.web.Dtos.DraftRequest;
import dev.cloudnine.web.Dtos.MessageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class GuestMessageService {

    private final GuestMessageRepository messages;
    private final PropertyRepository properties;
    private final PropertyService propertyService;
    private final KnowledgeIndex knowledgeIndex;
    private final ReplyDrafter drafter;

    public GuestMessageService(
            GuestMessageRepository messages,
            PropertyRepository properties,
            PropertyService propertyService,
            KnowledgeIndex knowledgeIndex,
            ReplyDrafter drafter
    ) {
        this.messages = messages;
        this.properties = properties;
        this.propertyService = propertyService;
        this.knowledgeIndex = knowledgeIndex;
        this.drafter = drafter;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listAll(UserAccount actor) {
        List<UUID> ids = propertyService.accessible(actor).stream().map(Property::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return messages.findForProperties(ids).stream().map(GuestMessageService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> listForProperty(UserAccount actor, UUID propertyId) {
        propertyService.requireReadable(actor, propertyId);
        return messages.findForProperty(propertyId).stream().map(GuestMessageService::toResponse).toList();
    }

    @Transactional
    public MessageResponse ask(UserAccount actor, UUID propertyId, AskRequest request) {
        propertyService.requireReadable(actor, propertyId);
        Property property = properties.findById(propertyId).orElseThrow(() -> ApiException.notFound("Property not found"));
        List<String> snippets = knowledgeIndex.search(propertyId, request.body(), 2);
        String draft = drafter.draft(property.getName(), request.guestName(), request.body(), snippets);

        GuestMessage message = new GuestMessage();
        message.setProperty(property);
        message.setGuestName(request.guestName().trim());
        message.setBody(request.body().trim());
        message.setDraftReply(draft);
        message.setEvidence(String.join("\n", snippets));
        message.setStatus(MessageStatus.DRAFT);
        return toResponse(messages.save(message));
    }

    @Transactional
    public MessageResponse updateDraft(UserAccount actor, UUID messageId, DraftRequest request) {
        GuestMessage message = requireDraft(actor, messageId);
        message.setDraftReply(request.reply().trim());
        return toResponse(message);
    }

    @Transactional
    public MessageResponse approve(UserAccount actor, UUID messageId, DraftRequest request) {
        GuestMessage message = requireDraft(actor, messageId);
        String reply = request.reply().trim();
        message.setDraftReply(reply);
        message.setSentReply(reply);
        message.setStatus(MessageStatus.SENT);
        message.setSentAt(Instant.now());
        return toResponse(message);
    }

    private GuestMessage requireDraft(UserAccount actor, UUID messageId) {
        GuestMessage message = messages.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Message not found"));
        propertyService.requireReadable(actor, message.getProperty().getId());
        if (message.getStatus() != MessageStatus.DRAFT) {
            throw ApiException.badRequest("That reply was already sent.");
        }
        return message;
    }

    static MessageResponse toResponse(GuestMessage message) {
        List<String> evidence = message.getEvidence() == null || message.getEvidence().isBlank()
                ? List.of()
                : Arrays.stream(message.getEvidence().split("\\n")).filter(line -> !line.isBlank()).toList();
        return new MessageResponse(
                message.getId(),
                message.getProperty().getId(),
                message.getProperty().getName(),
                message.getGuestName(),
                message.getBody(),
                message.getDraftReply(),
                message.getSentReply(),
                evidence,
                message.getStatus(),
                message.getCreatedAt(),
                message.getSentAt()
        );
    }
}
