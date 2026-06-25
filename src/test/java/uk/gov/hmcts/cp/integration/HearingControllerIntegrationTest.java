package uk.gov.hmcts.cp.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class HearingControllerIntegrationTest extends IntegrationTestBase {

    private static final String CASE_URN = "ABCD1234567";
    private static final UUID CASE_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID HEARING_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID DEFENDANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");

    private WireMockServer wireMockServer;

    @BeforeEach
    void beforeEach() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8081));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8081);
    }

    @AfterEach
    void afterEach() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void getCaseTimeline_should_returnOk_whenUpstreamHasHearings() throws Exception {
        stubUrnMapper(CASE_URN, CASE_ID);
        stubTimeline(CASE_ID, HTTP_OK, """
                {"hearingSummaries":[{"hearingId":"%s","hearingDate":"2026-06-23","hearingType":"First hearing","courtHouse":"Bexley Magistrates' Court","courtRoom":"Courtroom 01","hearingTime":"11:30","startTime":"10:30","outcome":null}]}
                """.formatted(HEARING_ID));

        mockMvc.perform(get("/hearings/cases/{caseURN}/timeline", CASE_URN)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hearingSummaries[0].id").value(HEARING_ID.toString()));
    }

    @Test
    void getCaseTimeline_should_return404_whenCaseUrnNotFound() throws Exception {
        stubUrnMapperNotFound(CASE_URN);

        mockMvc.perform(get("/hearings/cases/{caseURN}/timeline", CASE_URN)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void getCaseTimeline_should_return404_whenHearingQueryApiReturns404() throws Exception {
        stubUrnMapper(CASE_URN, CASE_ID);
        stubTimelineNotFound(CASE_ID);

        mockMvc.perform(get("/hearings/cases/{caseURN}/timeline", CASE_URN)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void getDefendantAttendance_should_returnOk_withMappedFields() throws Exception {
        stubGetHearing(HEARING_ID, HTTP_OK, """
                {"hearing":{"id":"%s","defendantAttendance":[{"defendantId":"%s","attendanceDays":[{"day":"2026-06-23","attendanceType":"IN_PERSON"}]}]}}
                """.formatted(HEARING_ID, DEFENDANT_ID));

        mockMvc.perform(get("/hearings/{hearingId}/attendance", HEARING_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(HEARING_ID.toString()))
                .andExpect(jsonPath("$.defendants[0].id").value(DEFENDANT_ID.toString()))
                .andExpect(jsonPath("$.defendants[0].attendanceDays[0].day").value("2026-06-23"))
                .andExpect(jsonPath("$.defendants[0].attendanceDays[0].type").value("IN_PERSON"));
    }

    @Test
    void getDefendantAttendance_should_returnEmptyDefendants_whenNoAttendanceData() throws Exception {
        stubGetHearing(HEARING_ID, HTTP_OK, """
                {"hearing":{"id":"%s","defendantAttendance":[]}}
                """.formatted(HEARING_ID));

        mockMvc.perform(get("/hearings/{hearingId}/attendance", HEARING_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defendants").isEmpty());
    }

    @Test
    void getDefendantAttendance_should_return404_whenHearingDoesNotExist() throws Exception {
        stubGetHearingNotFound(HEARING_ID);

        mockMvc.perform(get("/hearings/{hearingId}/attendance", HEARING_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    private void stubUrnMapper(final String caseUrn, final UUID caseId) {
        final String url = String.format("%s/%s", appProperties.getCaseMapperPath(), caseUrn);
        final String body = String.format("{\"caseUrn\":\"%s\",\"caseId\":\"%s\"}", caseUrn, caseId);
        log.info("Stubbing urnmapper url:{}", url);
        stubFor(WireMock.get(urlEqualTo(url)).willReturn(jsonResponse(HTTP_OK, body)));
    }

    private void stubUrnMapperNotFound(final String caseUrn) {
        final String url = String.format("%s/%s", appProperties.getCaseMapperPath(), caseUrn);
        log.info("Stubbing urnmapper not-found url:{}", url);
        stubFor(WireMock.get(urlEqualTo(url)).willReturn(aResponse().withStatus(HTTP_NOT_FOUND)));
    }

    private void stubTimeline(final UUID caseId, final int status, final String body) {
        final String url = String.format("%s/%s", appProperties.getHearingPath(), caseId);
        log.info("Stubbing timeline url:{}", url);
        stubFor(WireMock.get(urlEqualTo(url)).willReturn(jsonResponse(status, body)));
    }

    private void stubTimelineNotFound(final UUID caseId) {
        final String url = String.format("%s/%s", appProperties.getHearingPath(), caseId);
        log.info("Stubbing timeline not-found url:{}", url);
        stubFor(WireMock.get(urlEqualTo(url)).willReturn(aResponse().withStatus(HTTP_NOT_FOUND)));
    }

    private void stubGetHearing(final UUID hearingId, final int status, final String body) {
        final String url = String.format("%s/%s", appProperties.getHearingGetPath(), hearingId);
        log.info("Stubbing get-hearing url:{}", url);
        stubFor(WireMock.get(urlEqualTo(url)).willReturn(jsonResponse(status, body)));
    }

    private void stubGetHearingNotFound(final UUID hearingId) {
        final String url = String.format("%s/%s", appProperties.getHearingGetPath(), hearingId);
        log.info("Stubbing get-hearing not-found url:{}", url);
        stubFor(WireMock.get(urlEqualTo(url)).willReturn(aResponse().withStatus(HTTP_NOT_FOUND)));
    }

    private ResponseDefinitionBuilder jsonResponse(final int status, final String body) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }
}
