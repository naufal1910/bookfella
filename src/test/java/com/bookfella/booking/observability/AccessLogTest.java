package com.bookfella.booking.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test")
class AccessLogTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    void requestLogsContainJsonWithTraceUriStatus(CapturedOutput output) {
        String url = "http://localhost:" + port + "/api/health";
        ResponseEntity<String> resp = rest.getForEntity(url, String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        // Filter writes a single-line JSON; validate key fields exist
        String logs = output.getOut();
        assertThat(logs).contains("\"traceId\":");
        assertThat(logs).contains("\"uri\":\"/api/health\"");
        assertThat(logs).contains("\"status\":200");
        assertThat(logs).contains("\"elapsedMs\":");
    }
}
