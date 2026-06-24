package uk.gov.hmcts.cp.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.cp.openapi.api.HearingsApi;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendanceView;
import uk.gov.hmcts.cp.openapi.model.DefendantView;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;
import uk.gov.hmcts.cp.services.HearingService;

import java.util.List;
import java.util.UUID;

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

    @Override
    @NonNull
    public ResponseEntity<DefendantAttendanceView> getDefendantAttendance(final UUID hearingId) {
        log.info("Received request to get defendant attendance for hearingId:{}", Encode.forJava(String.valueOf(hearingId)));
        final DefendantAttendanceView attendanceView = hearingService.getDefendantAttendance(hearingId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(attendanceView);
    }

    @Override
    @NonNull
    public ResponseEntity<List<DefendantView>> getDefendants(final UUID hearingId, final String caseURN, final UUID masterDefendantId) {
        log.info("Received request to get defendants for hearingId:{} caseURN:{}",
                Encode.forJava(String.valueOf(hearingId)), Encode.forJava(caseURN));
        final List<DefendantView> defendants = hearingService.getDefendants(hearingId, caseURN, masterDefendantId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(defendants);
    }
}