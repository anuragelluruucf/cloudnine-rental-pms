package dev.stayledger;

import dev.stayledger.support.ApiTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RagFallbackIT {

    @Autowired
    MockMvc mvc;

    @Test
    void offlineDraftUsesListingNotesAndStaysUnsentUntilApproved() throws Exception {
        String token = ApiTests.register(mvc, "rag-" + UUID.randomUUID() + "@cloudnine.test");
        UUID propertyId = ApiTests.createProperty(mvc, token, "Pinecone Cabin", "pinecone-1842");

        MvcResult drafted = mvc.perform(post("/api/properties/" + propertyId + "/messages")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"guestName":"Sam Rivera","body":"What is the wifi password?"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.sentReply").value(nullValue()))
                .andExpect(jsonPath("$.draftReply", containsString("pinecone-1842")))
                .andExpect(jsonPath("$.evidence[0]", containsString("pinecone-1842")))
                .andReturn();

        String messageId = com.jayway.jsonpath.JsonPath.read(drafted.getResponse().getContentAsString(), "$.id");
        String edited = "Hello Sam, the wifi network is Test-Guest and the password is pinecone-1842.";

        mvc.perform(put("/api/messages/" + messageId + "/draft")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reply":"%s"}
                                """.formatted(edited)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.draftReply").value(edited));

        mvc.perform(post("/api/messages/" + messageId + "/approve")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reply":"%s"}
                                """.formatted(edited)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentReply").value(edited));

        mvc.perform(post("/api/messages/" + messageId + "/approve")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reply":"%s"}
                                """.formatted(edited)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wifiQuestionDoesNotSurfaceTheDoorCode() throws Exception {
        String token = ApiTests.register(mvc, "rag-door-" + UUID.randomUUID() + "@cloudnine.test");
        UUID propertyId = ApiTests.createProperty(mvc, token, "Lockbox Cabin", "pinecone-1842");

        mvc.perform(post("/api/properties/" + propertyId + "/messages")
                        .header("Authorization", ApiTests.bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"guestName":"Sam Rivera","body":"What is the wifi password?"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.draftReply", containsString("pinecone-1842")))
                .andExpect(jsonPath("$.draftReply", not(containsString("4419"))))
                .andExpect(jsonPath("$.evidence[0]", containsString("pinecone-1842")))
                .andExpect(jsonPath("$.evidence", not(hasItem(containsString("4419")))))
                .andExpect(jsonPath("$.evidence", not(hasItem(containsString("Lockbox")))))
                .andExpect(jsonPath("$.evidence", not(hasItem(containsString("Check-out")))))
                .andExpect(jsonPath("$.evidence", not(hasItem(containsString("small test cabin")))));
    }
}
