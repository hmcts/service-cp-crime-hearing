package uk.gov.hmcts.cp.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.clients.HearingClient;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.domain.HearingResponse.DefendantEntry;
import uk.gov.hmcts.cp.domain.HearingResponse.HearingDetail;
import uk.gov.hmcts.cp.domain.HearingResponse.ProsecutionCase;
import uk.gov.hmcts.cp.domain.HearingResponse.ProsecutionCaseIdentifier;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;
import uk.gov.hmcts.cp.mappers.DefendantAttendanceMapper;
import uk.gov.hmcts.cp.mappers.DefendantMapper;
import uk.gov.hmcts.cp.mappers.HearingMapper;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendanceView;
import uk.gov.hmcts.cp.openapi.model.DefendantView;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HearingServiceTest {

    @Mock
    private CaseUrnMapperService caseUrnMapperService;
    @Mock
    private HearingClient hearingClient;
    @Mock
    private HearingMapper hearingMapper;
    @Mock
    private DefendantAttendanceMapper defendantAttendanceMapper;
    @Mock
    private DefendantMapper defendantMapper;

    @InjectMocks
    private HearingService hearingService;

    private static final String CASE_URN_A = "ABCD1234567";
    private static final String CASE_URN_B = "WXYZ7654321";
    private static final UUID DEFENDANT_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID DEFENDANT_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000023");
    private static final UUID MASTER_DEFENDANT_ID_1 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID MASTER_DEFENDANT_ID_2 = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final String caseUrn = CASE_URN_A;
    private final UUID caseId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private final UUID hearingId = UUID.fromString("00000000-0000-0000-0000-000000000011");

    @Test
    void getCaseTimeline_should_resolveCaseIdThenFetchAndMapTimeline() {
        HearingTimelineResponse timelineResponse = HearingTimelineResponse.builder().build();
        HearingTimelineView expectedView = HearingTimelineView.builder().build();

        when(caseUrnMapperService.getCaseId(caseUrn)).thenReturn(caseId);
        when(hearingClient.getTimeline(caseId)).thenReturn(timelineResponse);
        when(hearingMapper.mapToHearingTimelineView(timelineResponse)).thenReturn(expectedView);

        HearingTimelineView result = hearingService.getCaseTimeline(caseUrn);

        assertEquals(expectedView, result);
        verify(caseUrnMapperService).getCaseId(caseUrn);
        verify(hearingClient).getTimeline(caseId);
        verify(hearingMapper).mapToHearingTimelineView(timelineResponse);
    }

    @Test
    void getDefendantAttendance_should_fetchHearingThenMap() {
        HearingResponse hearingResponse = HearingResponse.builder().build();
        DefendantAttendanceView expectedView = DefendantAttendanceView.builder().id(hearingId).build();

        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);
        when(defendantAttendanceMapper.mapToDefendantAttendanceView(hearingId, hearingResponse)).thenReturn(expectedView);

        DefendantAttendanceView result = hearingService.getDefendantAttendance(hearingId);

        assertEquals(expectedView, result);
        verify(hearingClient).getHearing(hearingId);
        verify(defendantAttendanceMapper).mapToDefendantAttendanceView(hearingId, hearingResponse);
    }

    @Test
    void getDefendants_should_returnMappedDefendant_whenMasterDefendantIdMatches() {
        DefendantEntry matchingDefendant = DefendantEntry.builder().id(DEFENDANT_ID_1).masterDefendantId(MASTER_DEFENDANT_ID_1).build();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN(CASE_URN_A).build())
                                        .defendants(List.of(matchingDefendant))
                                        .build()
                        ))
                        .build())
                .build();
        List<DefendantView> expectedViews = List.of(DefendantView.builder().id(matchingDefendant.getId()).build());
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);
        when(defendantMapper.mapToDefendantViews(List.of(matchingDefendant))).thenReturn(expectedViews);

        List<DefendantView> result = hearingService.getDefendants(hearingId, CASE_URN_A, MASTER_DEFENDANT_ID_1);

        assertThat(result).isEqualTo(expectedViews);
    }

    @Test
    void getDefendants_should_returnAllDefendantsInCase_whenMasterDefendantIdNotGiven() {
        DefendantEntry defendantOne = DefendantEntry.builder().id(DEFENDANT_ID_1).masterDefendantId(MASTER_DEFENDANT_ID_1).build();
        DefendantEntry defendantTwo = DefendantEntry.builder().id(DEFENDANT_ID_2).masterDefendantId(MASTER_DEFENDANT_ID_2).build();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN(CASE_URN_A).build())
                                        .defendants(List.of(defendantOne, defendantTwo))
                                        .build()
                        ))
                        .build())
                .build();
        List<DefendantView> expectedViews = List.of(
                DefendantView.builder().id(defendantOne.getId()).build(),
                DefendantView.builder().id(defendantTwo.getId()).build()
        );
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);
        when(defendantMapper.mapToDefendantViews(List.of(defendantOne, defendantTwo))).thenReturn(expectedViews);

        List<DefendantView> result = hearingService.getDefendants(hearingId, CASE_URN_A, null);

        assertThat(result).isEqualTo(expectedViews);
    }

    @Test
    void getDefendants_should_ignoreOtherCases_whenCaseUrnGiven() {
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN(CASE_URN_B).build())
                                        .defendants(List.of(
                                                DefendantEntry.builder().id(DEFENDANT_ID_1).masterDefendantId(MASTER_DEFENDANT_ID_1).build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);
        when(defendantMapper.mapToDefendantViews(List.of())).thenReturn(List.of());

        List<DefendantView> result = hearingService.getDefendants(hearingId, CASE_URN_A, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getDefendants_should_excludeDefendants_whenMasterDefendantIdDoesNotMatch() {
        DefendantEntry matchingDefendant = DefendantEntry.builder().id(DEFENDANT_ID_1).masterDefendantId(MASTER_DEFENDANT_ID_1).build();
        DefendantEntry otherDefendant = DefendantEntry.builder().id(DEFENDANT_ID_2).masterDefendantId(MASTER_DEFENDANT_ID_2).build();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN(CASE_URN_A).build())
                                        .defendants(List.of(matchingDefendant, otherDefendant))
                                        .build()
                        ))
                        .build())
                .build();
        List<DefendantView> expectedViews = List.of(DefendantView.builder().id(matchingDefendant.getId()).build());
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);
        when(defendantMapper.mapToDefendantViews(List.of(matchingDefendant))).thenReturn(expectedViews);

        List<DefendantView> result = hearingService.getDefendants(hearingId, CASE_URN_A, matchingDefendant.getMasterDefendantId());

        assertThat(result).isEqualTo(expectedViews);
    }

    @Test
    void getDefendants_should_returnEmpty_whenNoProsecutionCases() {
        when(hearingClient.getHearing(hearingId)).thenReturn(HearingResponse.builder()
                .hearing(HearingDetail.builder().build())
                .build());
        when(defendantMapper.mapToDefendantViews(List.of())).thenReturn(List.of());

        List<DefendantView> result = hearingService.getDefendants(hearingId, CASE_URN_A, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getDefendant_should_returnMappedView_whenDefendantFound() {
        DefendantEntry matchingDefendant = DefendantEntry.builder()
                .id(DEFENDANT_ID_1)
                .masterDefendantId(MASTER_DEFENDANT_ID_1)
                .build();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN(CASE_URN_A).build())
                                        .defendants(List.of(matchingDefendant))
                                        .build()
                        ))
                        .build())
                .build();
        DefendantView expectedView = DefendantView.builder().id(DEFENDANT_ID_1).build();
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);
        when(defendantMapper.mapToDefendantView(matchingDefendant)).thenReturn(expectedView);

        DefendantView result = hearingService.getDefendant(hearingId, CASE_URN_A, DEFENDANT_ID_1);

        assertThat(result).isEqualTo(expectedView);
    }

    @Test
    void getDefendant_should_throw404_whenDefendantIdNotFound() {
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN(CASE_URN_A).build())
                                        .defendants(List.of(
                                                DefendantEntry.builder().id(DEFENDANT_ID_2).masterDefendantId(MASTER_DEFENDANT_ID_2).build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> hearingService.getDefendant(hearingId, CASE_URN_A, DEFENDANT_ID_1));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}