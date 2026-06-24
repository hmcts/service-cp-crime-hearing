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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private final String caseUrn = "test-case-urn";
    private final UUID caseId = UUID.fromString("7a2e94c4-38af-43dd-906b-40d632d159b0");
    private final UUID hearingId = UUID.fromString("edcfb790-d709-4d57-8e30-6d2e0546a459");

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
        UUID masterDefendantId = UUID.randomUUID();
        DefendantEntry matchingDefendant = DefendantEntry.builder().id(UUID.randomUUID()).masterDefendantId(masterDefendantId).build();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN("test-case-urn-a").build())
                                        .defendants(List.of(matchingDefendant))
                                        .build()
                        ))
                        .build())
                .build();
        List<DefendantView> expectedViews = List.of(DefendantView.builder().id(matchingDefendant.getId()).build());
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);
        when(defendantMapper.mapToDefendantViews(List.of(matchingDefendant))).thenReturn(expectedViews);

        List<DefendantView> result = hearingService.getDefendants(hearingId, "test-case-urn-a", masterDefendantId);

        assertThat(result).isEqualTo(expectedViews);
    }

    @Test
    void getDefendants_should_returnAllDefendantsInCase_whenMasterDefendantIdNotGiven() {
        DefendantEntry defendantOne = DefendantEntry.builder().id(UUID.randomUUID()).masterDefendantId(UUID.randomUUID()).build();
        DefendantEntry defendantTwo = DefendantEntry.builder().id(UUID.randomUUID()).masterDefendantId(UUID.randomUUID()).build();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN("test-case-urn-a").build())
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

        List<DefendantView> result = hearingService.getDefendants(hearingId, "test-case-urn-a", null);

        assertThat(result).isEqualTo(expectedViews);
    }

    @Test
    void getDefendants_should_ignoreOtherCases_whenCaseUrnGiven() {
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN("test-case-urn-b").build())
                                        .defendants(List.of(
                                                DefendantEntry.builder().id(UUID.randomUUID()).masterDefendantId(UUID.randomUUID()).build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);
        when(defendantMapper.mapToDefendantViews(List.of())).thenReturn(List.of());

        List<DefendantView> result = hearingService.getDefendants(hearingId, "test-case-urn-a", null);

        assertThat(result).isEmpty();
    }

    @Test
    void getDefendants_should_excludeDefendants_whenMasterDefendantIdDoesNotMatch() {
        DefendantEntry matchingDefendant = DefendantEntry.builder().id(UUID.randomUUID()).masterDefendantId(UUID.randomUUID()).build();
        DefendantEntry otherDefendant = DefendantEntry.builder().id(UUID.randomUUID()).masterDefendantId(UUID.randomUUID()).build();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN("test-case-urn-a").build())
                                        .defendants(List.of(matchingDefendant, otherDefendant))
                                        .build()
                        ))
                        .build())
                .build();
        List<DefendantView> expectedViews = List.of(DefendantView.builder().id(matchingDefendant.getId()).build());
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);
        when(defendantMapper.mapToDefendantViews(List.of(matchingDefendant))).thenReturn(expectedViews);

        List<DefendantView> result = hearingService.getDefendants(hearingId, "test-case-urn-a", matchingDefendant.getMasterDefendantId());

        assertThat(result).isEqualTo(expectedViews);
    }

    @Test
    void getDefendants_should_returnEmpty_whenNoProsecutionCases() {
        when(hearingClient.getHearing(hearingId)).thenReturn(HearingResponse.builder()
                .hearing(HearingDetail.builder().build())
                .build());
        when(defendantMapper.mapToDefendantViews(List.of())).thenReturn(List.of());

        List<DefendantView> result = hearingService.getDefendants(hearingId, "test-case-urn-a", null);

        assertThat(result).isEmpty();
    }
}
