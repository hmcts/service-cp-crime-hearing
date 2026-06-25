package uk.gov.hmcts.cp.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendanceView;
import uk.gov.hmcts.cp.openapi.model.DefendantView;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;
import uk.gov.hmcts.cp.services.HearingService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingControllerTest {

    private static final String CASE_URN = "ABCD1234567";
    private static final UUID HEARING_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID DEFENDANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID MASTER_DEFENDANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private HearingService hearingService;

    @InjectMocks
    private HearingController hearingController;

    @Test
    void getCaseTimeline_should_returnOkWithTimelineView() {
        HearingTimelineView expectedView = HearingTimelineView.builder().build();
        when(hearingService.getCaseTimeline(CASE_URN)).thenReturn(expectedView);

        ResponseEntity<HearingTimelineView> response = hearingController.getCaseTimeline(CASE_URN);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedView);
    }

    @Test
    void getCaseTimeline_should_sanitizeCaseUrn() {
        String unsanitizedCaseUrn = "<script>alert('xss')</script>";
        HearingTimelineView expectedView = HearingTimelineView.builder().build();
        when(hearingService.getCaseTimeline(anyString())).thenReturn(expectedView);

        ResponseEntity<HearingTimelineView> response = hearingController.getCaseTimeline(unsanitizedCaseUrn);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getDefendantAttendance_should_returnOkWithAttendanceView() {
        DefendantAttendanceView expectedView = DefendantAttendanceView.builder().id(HEARING_ID).build();
        when(hearingService.getDefendantAttendance(HEARING_ID)).thenReturn(expectedView);

        ResponseEntity<DefendantAttendanceView> response = hearingController.getDefendantAttendance(HEARING_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedView);
    }

    @Test
    void getDefendants_should_returnOkWithDefendantViews() {
        List<DefendantView> expectedViews = List.of(DefendantView.builder().id(DEFENDANT_ID).masterDefendantId(MASTER_DEFENDANT_ID).build());
        when(hearingService.getDefendants(HEARING_ID, CASE_URN, MASTER_DEFENDANT_ID)).thenReturn(expectedViews);

        ResponseEntity<List<DefendantView>> response = hearingController.getDefendants(HEARING_ID, CASE_URN, MASTER_DEFENDANT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedViews);
    }
}