package de.marvinrichter.bba;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BranchByAbstractionIntegrationTest {

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "feature.new-order-service=false")
    static class LegacyAdapterTest {

        @Autowired
        MockMvc mockMvc;

        @Test
        void legacy_adapter_handles_requests_by_default() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"customerId": "cust-1", "totalAmount": "49.99"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }
    }

    @SpringBootTest
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "feature.new-order-service=true")
    static class NewAdapterTest {

        @Autowired
        MockMvc mockMvc;

        @Test
        void new_jpa_adapter_handles_requests_when_flag_enabled() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"customerId": "cust-1", "totalAmount": "49.99"}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        void rejects_zero_amount() throws Exception {
            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"customerId": "cust-1", "totalAmount": "0"}
                                    """))
                    .andExpect(status().is5xxServerError()); // domain validation throws
        }
    }
}
