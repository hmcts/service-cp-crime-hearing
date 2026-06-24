package uk.gov.hmcts.cp.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.domain.HearingResponse.HearingDetail;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse.HearingSummary;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingClientTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private AppPropertiesBackend appProperties;

    @InjectMocks
    private HearingClient hearingClient;

    private final UUID caseId = UUID.fromString("7a2e94c4-38af-43dd-906b-40d632d159b0");

    @Test
    void getTimeline_should_returnTimelineResponse_whenResponseIsSuccessful() {
        when(appProperties.getHearingUrl()).thenReturn("http://mock-server");
        when(appProperties.getHearingPath()).thenReturn("/hearing-query-api/query/api/rest/hearing/timeline");
        when(appProperties.getHearingCjscppuid()).thenReturn("test-cjscppuid");
        HearingTimelineResponse response = HearingTimelineResponse.builder()
                .hearingSummaries(List.of(HearingSummary.builder().hearingId(UUID.randomUUID()).build()))
                .build();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(HearingTimelineResponse.class)
        )).thenReturn(ResponseEntity.ok(response));

        HearingTimelineResponse result = hearingClient.getTimeline(caseId);

        assertThat(result.getHearingSummaries()).hasSize(1);
    }

    @Test
    void getHearing_should_returnHearingResponse_whenResponseIsSuccessful() {
        when(appProperties.getHearingUrl()).thenReturn("http://mock-server");
        when(appProperties.getHearingGetPath()).thenReturn("/hearing-query-api/query/api/rest/hearing/hearings");
        when(appProperties.getHearingCjscppuid()).thenReturn("test-cjscppuid");
        HearingResponse response = HearingResponse.builder()
                .hearing(HearingDetail.builder().build())
                .build();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(HearingResponse.class)
        )).thenReturn(ResponseEntity.ok(response));

        HearingResponse result = hearingClient.getHearing(caseId);

        assertThat(result.getHearing()).isNotNull();
    }
}
