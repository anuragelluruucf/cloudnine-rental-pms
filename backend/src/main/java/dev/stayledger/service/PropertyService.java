package dev.stayledger.service;

import dev.stayledger.domain.Property;
import dev.stayledger.domain.Role;
import dev.stayledger.domain.UserAccount;
import dev.stayledger.repo.ChannelSourceRepository;
import dev.stayledger.repo.GuestMessageRepository;
import dev.stayledger.repo.KnowledgeChunkRepository;
import dev.stayledger.repo.PropertyRepository;
import dev.stayledger.repo.ReservationRepository;
import dev.stayledger.web.ApiException;
import dev.stayledger.web.Dtos.PropertyRequest;
import dev.stayledger.web.Dtos.PropertyResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PropertyService {

    private final PropertyRepository properties;
    private final ReservationRepository reservations;
    private final ChannelSourceRepository channels;
    private final GuestMessageRepository messages;
    private final KnowledgeChunkRepository chunks;
    private final KnowledgeIndex knowledgeIndex;

    public PropertyService(
            PropertyRepository properties,
            ReservationRepository reservations,
            ChannelSourceRepository channels,
            GuestMessageRepository messages,
            KnowledgeChunkRepository chunks,
            @Lazy KnowledgeIndex knowledgeIndex
    ) {
        this.properties = properties;
        this.reservations = reservations;
        this.channels = channels;
        this.messages = messages;
        this.chunks = chunks;
        this.knowledgeIndex = knowledgeIndex;
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> list(UserAccount actor) {
        List<Property> rows = actor.getRole() == Role.ADMIN
                ? properties.findAll()
                : properties.findByOwnerIdOrderByNameAsc(actor.getId());
        return rows.stream().map(PropertyService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PropertyResponse get(UserAccount actor, UUID id) {
        return toResponse(requireReadable(actor, id));
    }

    @Transactional
    public PropertyResponse create(UserAccount actor, PropertyRequest request) {
        Property property = new Property();
        property.setOwner(actor);
        apply(property, request);
        properties.save(property);
        knowledgeIndex.syncProperty(property);
        return toResponse(property);
    }

    @Transactional
    public PropertyResponse update(UserAccount actor, UUID id, PropertyRequest request) {
        Property property = requireReadable(actor, id);
        apply(property, request);
        properties.save(property);
        knowledgeIndex.syncProperty(property);
        return toResponse(property);
    }

    @Transactional
    public void delete(UserAccount actor, UUID id) {
        Property property = requireReadable(actor, id);
        messages.deleteByPropertyId(id);
        reservations.deleteByPropertyId(id);
        channels.deleteByPropertyId(id);
        chunks.deleteByPropertyId(id);
        properties.deleteById(id);
        knowledgeIndex.rebuild();
    }

    @Transactional(readOnly = true)
    public Property requireReadable(UserAccount actor, UUID id) {
        Property property = properties.findById(id).orElseThrow(() -> ApiException.notFound("Property not found"));
        if (actor.getRole() != Role.ADMIN && !property.getOwner().getId().equals(actor.getId())) {
            throw ApiException.forbidden("You don't manage this property.");
        }
        return property;
    }

    @Transactional(readOnly = true)
    public List<Property> accessible(UserAccount actor) {
        if (actor.getRole() == Role.ADMIN) {
            return properties.findAll();
        }
        return properties.findByOwnerIdOrderByNameAsc(actor.getId());
    }

    private static void apply(Property property, PropertyRequest request) {
        property.setName(request.name().trim());
        property.setAddress(request.address().trim());
        property.setDescription(clean(request.description()));
        property.setHouseRules(clean(request.houseRules()));
        property.setAmenities(clean(request.amenities()));
        property.setCheckInInfo(clean(request.checkInInfo()));
        property.setCheckOutInfo(clean(request.checkOutInfo()));
        property.setWifiNetwork(clean(request.wifiNetwork()));
        property.setWifiPassword(clean(request.wifiPassword()));
        property.setFaqNotes(clean(request.faqNotes()));
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static PropertyResponse toResponse(Property property) {
        return new PropertyResponse(
                property.getId(),
                property.getName(),
                property.getAddress(),
                property.getDescription(),
                property.getHouseRules(),
                property.getAmenities(),
                property.getCheckInInfo(),
                property.getCheckOutInfo(),
                property.getWifiNetwork(),
                property.getWifiPassword(),
                property.getFaqNotes(),
                property.getExportToken(),
                "/api/ical/" + property.getExportToken() + "/calendar.ics",
                property.getUpdatedAt()
        );
    }
}
