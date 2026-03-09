package og.portal.zarplata.controller;

import og.portal.zarplata.ZarplataApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = ZarplataApplication.class
)
@AutoConfigureMockMvc
@WithMockUser(roles = "ORDER_GENERATOR")
@TestPropertySource(properties = {
        "app.share.folder.path=target/test-classes/",
        "app.purchase.folder.path=og/portal/zarplata/service"
})
public class PurchaseOrderIT {
    private static final String EXPECTED_HEALTH_RESPONSE_BODY = """
            {
              "status": "UP"
            }""";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json(EXPECTED_HEALTH_RESPONSE_BODY));
    }

    @Test
    void generateOrders() throws Exception {
        String file1 = "file1.xlsx";
        String file2 = "file2.xlsx";
        var request = """
                {
                    "currentPath": null,
                    "fileNames": [
                        "%s",
                        "%s"
                    ]
                }
                """.formatted(file1, file2);
        
        mockMvc.perform(post("/purchase-orders/generate")
                        .with(csrf())
                        .content(request)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void dashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
