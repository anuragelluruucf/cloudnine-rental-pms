package dev.stayledger;

import dev.stayledger.support.ApiTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationOverlapIT {

    @Autowired
    MockMvc mvc;

    @Test
    void overlapIsRejectedAndCheckoutDayIsFree() throws Exception {
        String token = ApiTests.register(mvc, "overlap-" + UUID.randomUUID() + "@cloudnine.test");
        UUID propertyId = ApiTests.createProperty(mvc, token, "Overlap Cabin", "overlap-pass");

        book(token, propertyId, "2027-03-01", "2027-03-05", false).andExpect(status().isCreated());

        book(token, propertyId, "2027-03-04", "2027-03-08", false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("OVERLAP"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Check-in counts")));

        book(token, propertyId, "2027-03-05", "2027-03-08", false)
                .andExpect(status().isCreated());

        book(token, propertyId, "2027-02-27", "2027-03-01", false)
                .andExpect(status().isCreated());

        book(token, propertyId, "2027-03-01", "2027-03-02", true)
                .andExpect(status().isConflict());
    }

    private org.springframework.test.web.servlet.ResultActions book(
            String token, UUID propertyId, String checkIn, String checkOut, boolean block) throws Exception {
        return mvc.perform(post("/api/properties/" + propertyId + "/reservations")
                .header("Authorization", ApiTests.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "guestName":"%s",
                          "checkIn":"%s",
                          "checkOut":"%s",
                          "channel":"DIRECT",
                          "block":%s
                        }
                        """.formatted(block ? "Blocked" : "Ada Lovelace", checkIn, checkOut, block)));
    }
}
