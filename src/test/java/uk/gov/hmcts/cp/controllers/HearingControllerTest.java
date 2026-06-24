package uk.gov.hmcts.cp.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendanceView;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;
import uk.gov.hmcts.cp.services.HearingService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingControllerTest {

    @Mock
    private HearingService hearingService;

    @InjectMocks
    private HearingController hearingController;

    @Test
    void getCaseTimeline_should_returnOkWithTimelineView() {
        String caseUrn = "test-case-urn";
        HearingTimelineView expectedView = HearingTimelineView.builder().build();
        when(hearingService.getCaseTimeline(caseUrn)).thenReturn(expectedView);

        ResponseEntity<HearingTimelineView> response = hearingController.getCaseTimeline(caseUrn);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedView);
    }

    @Test
    void getCaseTimeline_should_sanitizeCaseUrn() {
        String unsanitizedCaseUrn = "<script>alert('xss')</script>";
        HearingTimelineView expectedView = HearingTimelineView.builder().build();
        lenient().when(hearingService.getCaseTimeline(anyString())).thenReturn(expectedView);

        ResponseEntity<HearingTimelineView> response = hearingController.getCaseTimeline(unsanitizedCaseUrn);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getDefendantAttendance_should_returnOkWithAttendanceView() {
        UUID hearingId = UUID.randomUUID();
        DefendantAttendanceView expectedView = DefendantAttendanceView.builder().id(hearingId).build();
        when(hearingService.getDefendantAttendance(hearingId)).thenReturn(expectedView);

        ResponseEntity<DefendantAttendanceView> response = hearingController.getDefendantAttendance(hearingId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedView);
    }
}