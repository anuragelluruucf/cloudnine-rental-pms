package dev.stayledger.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class ApiTests {

    private ApiTests() {
    }

    public static String register(MockMvc mvc, String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test Host","email":"%s","password":"cloudnine-demo"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    public static UUID createProperty(MockMvc mvc, String token, String name, String wifiPassword) throws Exception {
        MvcResult result = mvc.perform(post("/api/properties")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"%s",
                                  "address":"1 Test Lane",
                                  "description":"A small test cabin.",
                                  "houseRules":"Quiet hours are 10pm to 8am. No smoking.",
                                  "amenities":"Grill on the back deck.",
                                  "checkInInfo":"Check-in is at 4pm. Lockbox code 4419.",
                                  "checkOutInfo":"Check-out is at 10am.",
                                  "wifiNetwork":"Test-Guest",
                                  "wifiPassword":"%s",
                                  "faqNotes":"Trash pickup is Thursday morning."
                                }
                                """.formatted(name, wifiPassword)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }
}
