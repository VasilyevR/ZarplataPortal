package og.portal.zarplata.controller;

import og.portal.zarplata.TestConfiguration;
import og.portal.zarplata.ZarplataApplication;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {ZarplataApplication.class, TestConfiguration.class}
)

@TestPropertySource(properties = {
        "app.share.folder.path=target/test-classes/",
        "app.purchase.folder.path=og/portal/zarplata/service"
})
public class PurchaseOrderIT {
    private static final String URI_FORMAT = "http://localhost:%d/%s";
    private static final String EXPECTED_HEALTH_RESPONSE_BODY = """
            {
              "status": "UP"
            }""";

    @Value("${local.server.port:8080}")
    private int port;

    @Value("${app.share.folder.path}")
    private String folderPath;

    @Value("${app.purchase.folder.path}")
    private String purchaseFolderPath;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.of(5L, ChronoUnit.SECONDS))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Test
    void contextLoads() throws Exception {
        var response = httpClient.send(
                HttpRequest.newBuilder(createUri("actuator/health")).GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        JSONAssert.assertEquals(EXPECTED_HEALTH_RESPONSE_BODY, response.body(), true);
    }

    private URI createUri(String resource) {
        return URI.create(URI_FORMAT.formatted(this.port, resource));
    }

    @Test
    void generateOrders() throws Exception {
        //given
        Path path = Path.of("test-classes")
                .resolve("og/portal/zarplata/service");
        String file1 = "file1.xlsx";
        String file2 = "file2.xlsx";
        var request = """
                {
                    "currentPath": "%s",
                    "currentPath": null,
                    "fileNames": [
                        "%s",
                        "%s"
                    ]
                }
                """.formatted(path, file1, file2);
        var response = httpClient.send(
                HttpRequest.newBuilder(createUri("purchase-orders/generate")).POST(
                                HttpRequest.BodyPublishers.ofString(request)
                        )
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(HttpStatus.OK.value(), response.statusCode());
    }

    @Test
    void dashboard() throws Exception {

        var response = httpClient.send(
                HttpRequest.newBuilder(createUri("")).GET()
                        .header(HttpHeaders.COOKIE, "JSESSIONID=jsessionid")
                        .header(HttpHeaders.AUTHORIZATION, "NTLM ntlm")
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(HttpStatus.OK.value(), response.statusCode());
    }
}
