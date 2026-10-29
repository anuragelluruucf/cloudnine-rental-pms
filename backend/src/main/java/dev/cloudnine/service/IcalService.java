package dev.cloudnine.service;

import dev.cloudnine.domain.ChannelSource;
import dev.cloudnine.domain.Property;
import dev.cloudnine.domain.Reservation;
import dev.cloudnine.domain.ReservationStatus;
import dev.cloudnine.domain.UserAccount;
import dev.cloudnine.ical.IcalParser;
import dev.cloudnine.ical.ParsedEvent;
import dev.cloudnine.repo.ChannelSourceRepository;
import dev.cloudnine.repo.PropertyRepository;
import dev.cloudnine.repo.ReservationRepository;
import dev.cloudnine.web.ApiException;
import dev.cloudnine.web.Dtos.ImportReport;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class IcalService {

    private static final Logger log = LoggerFactory.getLogger(IcalService.class);

    private final ChannelSourceRepository channels;
    private final ReservationRepository reservations;
    private final PropertyRepository properties;
    private final ReservationService reservationService;
    private final PropertyService propertyService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public IcalService(
            ChannelSourceRepository channels,
            ReservationRepository reservations,
            PropertyRepository properties,
            ReservationService reservationService,
            PropertyService propertyService
    ) {
        this.channels = channels;
        this.reservations = reservations;
        this.properties = properties;
        this.reservationService = reservationService;
        this.propertyService = propertyService;
    }

    @Transactional
    public ImportReport importFeed(UUID channelId, String ics) {
        if (channels.findById(channelId).isEmpty()) {
            throw ApiException.notFound("Channel not found");
        }
        List<ParsedEvent> events = IcalParser.parse(ics);
        int created = 0;
        int updated = 0;
        int conflicts = 0;
        int cancelled = 0;
        for (ParsedEvent event : events) {
            switch (reservationService.importExternal(channelId, event)) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case CONFLICT -> conflicts++;
                case CANCELLED -> cancelled++;
            }
        }
        String message = created + " new, " + updated + " updated, " + conflicts
                + " flagged as conflicts (existing stays were left alone), " + cancelled + " cancelled.";
        ChannelSource channel = channels.findById(channelId).orElseThrow();
        channel.setLastSyncedAt(Instant.now());
        channel.setLastSyncMessage(trim(message, 1000));
        return new ImportReport(created, updated, conflicts, cancelled, message);
    }

    public ImportReport sync(UserAccount actor, UUID channelId) {
        ChannelSource channel = channels.findById(channelId)
                .orElseThrow(() -> ApiException.notFound("Channel not found"));
        propertyService.requireReadable(actor, channel.getProperty().getId());
        if (channel.getImportUrl() == null || channel.getImportUrl().isBlank()) {
            throw ApiException.badRequest("Add an iCal URL, or import a calendar file.");
        }
        return syncUrl(channel);
    }

    public void syncAll() {
        for (ChannelSource channel : channels.findByEnabledTrue()) {
            if (channel.getImportUrl() == null || channel.getImportUrl().isBlank()) {
                continue;
            }
            String url = channel.getImportUrl().trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                continue;
            }
            try {
                syncUrl(channel);
            } catch (RuntimeException ex) {
                log.warn("iCal sync failed for channel {}: {}", channel.getId(), ex.getMessage());
                channel.setLastSyncedAt(Instant.now());
                channel.setLastSyncMessage(trim("Sync failed: " + ex.getMessage(), 1000));
                channels.save(channel);
            }
        }
    }

    @Transactional(readOnly = true)
    public String export(UserAccount actor, UUID propertyId) {
        propertyService.requireReadable(actor, propertyId);
        return render(propertyId);
    }

    @Transactional(readOnly = true)
    public String exportByToken(UUID token) {
        Property property = properties.findByExportToken(token)
                .orElseThrow(() -> ApiException.notFound("Calendar not found"));
        return render(property.getId());
    }

    private ImportReport syncUrl(ChannelSource channel) {
        String ics = fetch(channel.getImportUrl().trim());
        return importFeed(channel.getId(), ics);
    }

    private String fetch(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "CloudNine/0.1")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw ApiException.badRequest("Calendar URL returned " + response.statusCode());
            }
            return response.body();
        } catch (ApiException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ApiException.badRequest("Calendar download was interrupted.");
        } catch (Exception ex) {
            throw ApiException.badRequest("Could not download that calendar: " + ex.getMessage());
        }
    }

    private String render(UUID propertyId) {
        Property property = properties.findById(propertyId).orElseThrow(() -> ApiException.notFound("Property not found"));
        List<Reservation> stays = reservations.findByPropertyIdAndStatusIn(
                propertyId, List.of(ReservationStatus.CONFIRMED, ReservationStatus.BLOCKED));
        Calendar calendar = new Calendar();
        calendar.add(new ProdId("-//CloudNine//CloudNine 0.1//EN"));
        calendar.add(ImmutableVersion.VERSION_2_0);
        calendar.add(ImmutableCalScale.GREGORIAN);
        for (Reservation stay : stays) {
            String summary = stay.getGuestName() == null || stay.getGuestName().isBlank()
                    ? "Blocked"
                    : stay.getGuestName();
            VEvent event = new VEvent(stay.getCheckIn(), stay.getCheckOut(), summary);
            String uid = stay.getExternalUid() == null || stay.getExternalUid().isBlank()
                    ? stay.getId() + "@cloudnine"
                    : stay.getExternalUid();
            event.add(new Uid(uid));
            if (stay.getNotes() != null && !stay.getNotes().isBlank()) {
                event.add(new Description(stay.getNotes()));
            }
            calendar.add(event);
        }
        StringWriter writer = new StringWriter();
        try {
            new CalendarOutputter(false).output(calendar, writer);
        } catch (IOException | RuntimeException ex) {
            throw ApiException.badRequest("Could not build the calendar feed.");
        }
        return writer.toString();
    }

    private static String trim(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
