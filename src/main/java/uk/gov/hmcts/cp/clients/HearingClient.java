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
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HearingClient {

    private final AppPropertiesBackend appProperties;
    private final RestTemplate restTemplate;

    public HearingTimelineResponse getTimeline(final UUID caseId) {
        final String url = buildUrl(caseId);
        log.info("Getting hearing timeline from {}", Encode.forJava(url));
        final ResponseEntity<HearingTimelineResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                getRequestEntity(),
                HearingTimelineResponse.class
        );
        return response.getBody();
    }

    private String buildUrl(final UUID caseId) {
        return String.format("%s%s/%s", appProperties.getHearingUrl(), appProperties.getHearingPath(), caseId);
    }

    private HttpEntity<String> getRequestEntity() {
        final HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/vnd.hearing.case.timeline+json");
        headers.add("CJSCPPUID", appProperties.getHearingCjscppuid());
        return new HttpEntity<>(headers);
    }
}