package uk.gov.hmcts.cp.integration;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "management.tracing.enabled=true"
})
@AutoConfigureMockMvc
class TracingIntegrationTest {

    private static final String TRACE_ID_HEADER = "traceId";
    private static final String SPAN_ID_HEADER = "spanId";
    private static final String TEST_TRACE_ID = "1234-1234";
    private static final String TEST_SPAN_ID = "567-567";
    private static final String LOGGED_CONTROLLER = "uk.gov.hmcts.cp.controllers.HearingController";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${spring.application.name}")
    private String springApplicationName;

    @Resource
    private MockMvc mockMvc;

    private final PrintStream originalStdOut = System.out;

    @AfterEach
    void afterEach() {
        System.setOut(originalStdOut);
    }

    @Test
    void incomingRequestShouldAddNewTracing() throws Exception {
        final ByteArrayOutputStream capturedStdOut = captureStdOut();

        mockMvc.perform(get("/hearings/cases/{caseURN}/timeline", "test-case-urn"));

        final Map<String, Object> capturedFields = findLogFromHearingController(capturedStdOut);

        assertThat(capturedFields.get("logger_name")).isEqualTo(LOGGED_CONTROLLER);
        assertThat((String) capturedFields.get("message")).startsWith("Received request to get case timeline");
    }

    @Test
    void incomingRequestWithTraceIdShouldPassThrough() throws Exception {
        final ByteArrayOutputStream capturedStdOut = captureStdOut();
        final MvcResult result = mockMvc.perform(get("/hearings/cases/{caseURN}/timeline", "test-case-urn")
                        .header(TRACE_ID_HEADER, TEST_TRACE_ID)
                        .header(SPAN_ID_HEADER, TEST_SPAN_ID))
                .andDo(print())
                .andReturn();

        System.out.flush();

        assertThat(result.getResponse().getHeader(TRACE_ID_HEADER)).isEqualTo(TEST_TRACE_ID);
        assertThat(result.getResponse().getHeader(SPAN_ID_HEADER)).isEqualTo(TEST_SPAN_ID);

        final Map<String, Object> capturedFields = findLogWithTraceIdAndSpanId(capturedStdOut, TEST_TRACE_ID, TEST_SPAN_ID);
        assertThat(capturedFields.get(TRACE_ID_HEADER)).isEqualTo(TEST_TRACE_ID);
        assertThat(capturedFields.get(SPAN_ID_HEADER)).isEqualTo(TEST_SPAN_ID);
        assertThat(capturedFields.get("applicationName")).isEqualTo(springApplicationName);
    }

    private ByteArrayOutputStream captureStdOut() {
        final ByteArrayOutputStream capturedStdOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedStdOut));
        return capturedStdOut;
    }

    private Map<String, Object> findLogFromHearingController(final ByteArrayOutputStream buf) throws Exception {
        final String[] lines = buf.toString(java.nio.charset.StandardCharsets.UTF_8).split("\\R");

        for (int i = lines.length - 1; i >= 0; i--) {
            final String line = lines[i].trim();
            if (!line.isEmpty() && line.startsWith("{") && line.endsWith("}")) {
                try {
                    final Map<String, Object> parsed = OBJECT_MAPPER.readValue(line, new TypeReference<>() {
                    });
                    if (LOGGED_CONTROLLER.equals(parsed.get("logger_name"))) {
                        return parsed;
                    }
                } catch (Exception ignored) {
                    // skip non-JSON lines interleaved on stdout
                }
            }
        }

        throw new IllegalStateException("No JSON log line found from HearingController on STDOUT");
    }

    private Map<String, Object> findLogWithTraceIdAndSpanId(final ByteArrayOutputStream buf, final String expectedTraceId, final String expectedSpanId) throws Exception {
        final String[] lines = buf.toString(java.nio.charset.StandardCharsets.UTF_8).split("\\R");

        for (int i = lines.length - 1; i >= 0; i--) {
            final String line = lines[i].trim();
            if (!line.isEmpty() && line.startsWith("{") && line.endsWith("}")) {
                try {
                    final Map<String, Object> parsed = OBJECT_MAPPER.readValue(line, new TypeReference<>() {
                    });
                    if (expectedTraceId.equals(parsed.get(TRACE_ID_HEADER)) && expectedSpanId.equals(parsed.get(SPAN_ID_HEADER))) {
                        return parsed;
                    }
                } catch (Exception ignored) {
                    // skip non-JSON lines interleaved on stdout
                }
            }
        }

        throw new IllegalStateException("No JSON log line found with traceId=" + expectedTraceId + " and spanId=" + expectedSpanId + " on STDOUT");
    }
}