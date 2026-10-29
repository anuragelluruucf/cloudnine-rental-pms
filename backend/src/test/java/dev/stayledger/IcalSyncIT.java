package dev.stayledger;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import dev.stayledger.ical.IcalParser;
import dev.stayledger.repo.ReservationRepository;
import dev.stayledger.support.ApiTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IcalSyncIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    ReservationRepository reservations;

    @Test
    void importIsIdempotentMatchesChangedUidsAndFlagsConflicts() throws Exception {
        String token = ApiTests.register(mvc, "ical-" + UUID.randomUUID() + "@cloudnine.test");
        UUID propertyId = ApiTests.createProperty(mvc, token, "Calendar Cabin", "ical-pass");
        UUID channelId = createChannel(token, propertyId);
        String fixture = fixture("ical/airbnb-sample.ics");

        mvc.perform(post("/api/channels/" + channelId + "/import")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonIcs(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2));
        assertEquals(2, reservations.countByPropertyId(propertyId));

        mvc.perform(post("/api/channels/" + channelId + "/import")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonIcs(fixture)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(2));
        assertEquals(2, reservations.countByPropertyId(propertyId));

        String changedUids = fixture
                .replace("airbnb-sample-001@cloudnine.test", "airbnb-new-001@cloudnine.test")
                .replace("airbnb-sample-002@cloudnine.test", "airbnb-new-002@cloudnine.test");
        mvc.perform(post("/api/channels/" + channelId + "/import")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonIcs(changedUids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.updated").value(2));
        assertEquals(2, reservations.countByPropertyId(propertyId));
        assertTrue(reservations.findByPropertyIdOrderByCheckInAsc(propertyId).stream()
                .anyMatch(row -> "airbnb-new-001@cloudnine.test".equals(row.getExternalUid())));
    }

    @Test
    void overlappingImportIsFlaggedAndDoesNotOverwriteTheExistingStay() throws Exception {
        String token = ApiTests.register(mvc, "flag-" + UUID.randomUUID() + "@cloudnine.test");
        UUID propertyId = ApiTests.createProperty(mvc, token, "Flag Cabin", "flag-pass");
        UUID channelId = createChannel(token, propertyId);

        mvc.perform(post("/api/properties/" + propertyId + "/reservations")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName":"Local Guest",
                                  "checkIn":"2026-11-06",
                                  "checkOut":"2026-11-08",
                                  "channel":"DIRECT",
                                  "block":false
                                }
                                """))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/channels/" + channelId + "/import")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonIcs(fixture("ical/airbnb-sample.ics"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conflicts").value(1))
                .andExpect(jsonPath("$.created").value(1));

        var rows = reservations.findByPropertyIdOrderByCheckInAsc(propertyId);
        assertEquals(3, rows.size());
        var local = rows.stream().filter(row -> "Local Guest".equals(row.getGuestName())).findFirst().orElseThrow();
        assertEquals("CONFIRMED", local.getStatus().name());
        assertFalse(local.isConflict());
        assertEquals("2026-11-06", local.getCheckIn().toString());
        assertEquals("2026-11-08", local.getCheckOut().toString());

        var flagged = rows.stream().filter(row -> row.getExternalUid() != null && row.getExternalUid().contains("001")).findFirst().orElseThrow();
        assertTrue(flagged.isConflict());
        assertEquals("CONFLICT", flagged.getStatus().name());
    }

    @Test
    void exportRoundTripsAndUrlSyncImportsOnce() throws Exception {
        String token = ApiTests.register(mvc, "export-" + UUID.randomUUID() + "@cloudnine.test");
        UUID propertyId = ApiTests.createProperty(mvc, token, "Export Cabin", "export-pass");
        String fixture = fixture("ical/vrbo-sample.ics");

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/feed.ics", exchange -> {
            byte[] body = fixture.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/calendar");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/feed.ics";
            MvcResult created = mvc.perform(post("/api/properties/" + propertyId + "/channels")
                            .header("Authorization", ApiTests.bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"channel":"VRBO","importUrl":"%s"}
                                    """.formatted(url)))
                    .andExpect(status().isCreated())
                    .andReturn();
            String channelId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

            mvc.perform(post("/api/channels/" + channelId + "/sync")
                            .header("Authorization", ApiTests.bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.created").value(1));
            mvc.perform(post("/api/channels/" + channelId + "/sync")
                            .header("Authorization", ApiTests.bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.created").value(0))
                    .andExpect(jsonPath("$.updated").value(1));
            assertEquals(1, reservations.countByPropertyId(propertyId));
        } finally {
            server.stop(0);
        }

        MvcResult feed = mvc.perform(get("/api/properties/" + propertyId + "/calendar.ics")
                        .header("Authorization", ApiTests.bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        String ics = feed.getResponse().getContentAsString();
        assertTrue(ics.contains("BEGIN:VEVENT"));
        var parsed = IcalParser.parse(ics);
        assertEquals(1, parsed.size());
        assertEquals("2026-12-02", parsed.getFirst().checkIn().toString());
        assertEquals("2026-12-05", parsed.getFirst().checkOut().toString());

        String tokenPath = mvc.perform(get("/api/properties/" + propertyId)
                        .header("Authorization", ApiTests.bearer(token)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String path = JsonPath.read(tokenPath, "$.icalPath");
        mvc.perform(get(path))
                .andExpect(status().isOk());
    }

    private UUID createChannel(String token, UUID propertyId) throws Exception {
        MvcResult result = mvc.perform(post("/api/properties/" + propertyId + "/channels")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"channel":"AIRBNB"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private static String fixture(String path) throws Exception {
        try (var input = IcalSyncIT.class.getClassLoader().getResourceAsStream(path)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String jsonIcs(String ics) {
        return "{\"ics\":" + jsonString(ics) + "}";
    }

    private static String jsonString(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (char character : value.toCharArray()) {
            switch (character) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                default -> builder.append(character);
            }
        }
        return builder.append('"').toString();
    }
}
