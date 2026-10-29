package dev.cloudnine.ical;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IcalParserTest {

    @Test
    void parsesAirbnbAllDayEventsWithExclusiveEnd() throws Exception {
        String ics = fixture("ical/airbnb-sample.ics");
        List<ParsedEvent> events = IcalParser.parse(ics);

        assertEquals(2, events.size());
        ParsedEvent first = events.getFirst();
        assertEquals("airbnb-sample-001@cloudnine.test", first.uid());
        assertEquals(LocalDate.of(2026, 11, 6), first.checkIn());
        assertEquals(LocalDate.of(2026, 11, 9), first.checkOut());
        assertEquals("Reserved - Alex Kim", first.summary());

        ParsedEvent second = events.get(1);
        assertEquals(LocalDate.of(2026, 11, 18), second.checkIn());
        assertEquals(LocalDate.of(2026, 11, 22), second.checkOut());
        assertTrue(second.description().contains("still keeps this sentence together"));
    }

    @Test
    void parsesVrboFixture() throws Exception {
        List<ParsedEvent> events = IcalParser.parse(fixture("ical/vrbo-sample.ics"));
        assertEquals(1, events.size());
        assertEquals(LocalDate.of(2026, 12, 2), events.getFirst().checkIn());
        assertEquals(LocalDate.of(2026, 12, 5), events.getFirst().checkOut());
    }

    private static String fixture(String path) throws Exception {
        try (var input = IcalParserTest.class.getClassLoader().getResourceAsStream(path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
