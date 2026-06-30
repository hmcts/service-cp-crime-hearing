package uk.gov.hmcts.cp.clients;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HearingClient {

    private static final String TIMELINE_ACCEPT = "application/vnd.hearing.case.timeline+json";
    private static final String GET_HEARING_ACCEPT = "application/vnd.hearing.get.hearing+json";

    private final AppPropertiesBackend appProperties;
    private final RestTemplate restTemplate;

    public HearingTimelineResponse getTimeline(final UUID caseId) {
        final String url = String.format("%s%s/%s", appProperties.getHearingUrl(), appProperties.getHearingPath(), caseId);
        log.info("Getting hearing timeline from {}", Encode.forJava(url));
        final ResponseEntity<HearingTimelineResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getRequestEntity(TIMELINE_ACCEPT),
                HearingTimelineResponse.class
        );
        return response.getBody();
    }

    public HearingResponse getHearing(final UUID hearingId) {
        final String url = String.format("%s%s/%s", appProperties.getHearingUrl(), appProperties.getHearingGetPath(), hearingId);
        log.info("Getting hearing from {}", Encode.forJava(url));
        final ResponseEntity<HearingResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getRequestEntity(GET_HEARING_ACCEPT),
                HearingResponse.class
        );
        return response.getBody();
    }

    private HttpEntity<String> getRequestEntity(final String acceptHeader) {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", acceptHeader);
        headers.add("CJSCPPUID", appProperties.getHearingCjscppuid());
        return new HttpEntity<>(headers);
    }
}