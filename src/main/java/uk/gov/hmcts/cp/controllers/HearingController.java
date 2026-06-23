package uk.gov.hmcts.cp.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.openapi.api.HearingsApi;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;
import uk.gov.hmcts.cp.services.HearingService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class HearingController implements HearingsApi {

    private final HearingService hearingService;

    @Override
    @NonNull
    public ResponseEntity<HearingTimelineView> getCaseTimeline(final String caseURN) {
        log.info("Received request to get case timeline for caseURN:{}", Encode.forJava(caseURN));
        final HearingTimelineView timelineView = hearingService.getCaseTimeline(caseURN);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(timelineView);
    }
}