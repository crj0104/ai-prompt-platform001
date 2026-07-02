package com.course.promptplatform;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest
class AiPromptTemplatePlatformApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void templateSearchApiWorks() throws Exception {
        mockMvc.perform(get("/api/templates/search").param("keyword", "Java"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Java")));
    }

    @Test
    void templateDetailApiWorks() throws Exception {
        mockMvc.perform(get("/api/templates/102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(102))
                .andExpect(jsonPath("$.promptContent").isNotEmpty());
    }

    @Test
    void loginChecksPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"demo_user\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"demo_user\",\"password\":\"demo_hash\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void purchaseApiIsIdempotentForOwnedTemplate() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":102}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Template already purchased"));
    }

    @Test
    void useTemplateRequiresPurchaseForPaidTemplate() throws Exception {
        mockMvc.perform(post("/api/templates/103/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inputSummary\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unfavoriteApiUpdatesState() throws Exception {
        mockMvc.perform(delete("/api/templates/102/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
