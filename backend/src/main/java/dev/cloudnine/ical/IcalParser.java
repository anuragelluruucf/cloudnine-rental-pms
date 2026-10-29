package dev.cloudnine.ical;

import dev.cloudnine.CloudNineApplication;
import dev.cloudnine.web.ApiException;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;

public final class IcalParser {

    static {
        CloudNineApplication.relaxIcalParsing();
    }

    private IcalParser() {
    }

    public static List<ParsedEvent> parse(String ics) {
        if (ics == null || ics.isBlank()) {
            throw ApiException.badRequest("The calendar file is empty.");
        }
        try {
            Calendar calendar = new CalendarBuilder().build(new StringReader(ics));
            List<ParsedEvent> events = new ArrayList<>();
            for (Component component : calendar.getComponents()) {
                if (component instanceof VEvent event) {
                    ParsedEvent parsed = toEvent(event);
                    if (parsed != null) {
                        events.add(parsed);
                    }
                }
            }
            return events;
        } catch (IOException | ParserException ex) {
            throw ApiException.badRequest("Could not read that calendar: " + ex.getMessage());
        }
    }

    private static ParsedEvent toEvent(VEvent event) {
        DtStart<?> start = event.getDateTimeStart();
        if (start == null || start.getDate() == null) {
            return null;
        }
        LocalDate checkIn = toDate(start.getDate());
        LocalDate checkOut = endDate(event, checkIn);
        if (!checkOut.isAfter(checkIn)) {
            checkOut = checkIn.plusDays(1);
        }
        String summary = text(event, Property.SUMMARY);
        String description = text(event, Property.DESCRIPTION);
        String uid = event.getUid().map(Property::getValue).orElse("").trim();
        if (uid.isBlank()) {
            uid = "synthetic:" + checkIn + ":" + checkOut;
        }
        String status = text(event, Property.STATUS);
        boolean cancelled = "CANCELLED".equalsIgnoreCase(status);
        return new ParsedEvent(uid, checkIn, checkOut, summary, description, cancelled);
    }

    private static LocalDate endDate(VEvent event, LocalDate checkIn) {
        DtEnd<?> end = event.getDateTimeEnd();
        if (end != null && end.getDate() != null) {
            return toDate(end.getDate());
        }
        if (event.getDuration() != null && event.getDuration().getDuration() != null) {
            TemporalAmount amount = event.getDuration().getDuration();
            return checkIn.plus(amount);
        }
        return checkIn.plusDays(1);
    }

    private static String text(Component component, String name) {
        return component.getProperty(name).map(Property::getValue).orElse("").trim();
    }

    static LocalDate toDate(Temporal temporal) {
        return switch (temporal) {
            case LocalDate date -> date;
            case LocalDateTime dateTime -> dateTime.toLocalDate();
            case ZonedDateTime zoned -> zoned.toLocalDate();
            case OffsetDateTime offset -> offset.toLocalDate();
            case Instant instant -> instant.atZone(ZoneOffset.UTC).toLocalDate();
            default -> LocalDate.parse(temporal.toString().substring(0, 10));
        };
    }
}
