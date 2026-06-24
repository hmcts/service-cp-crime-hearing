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
import uk.gov.hmcts.cp.mappers.HearingMapper;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendanceView;
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
    void resolveDefendantIds_should_returnMatchingDefendantId_whenMasterDefendantIdMatches() {
        UUID masterDefendantId = UUID.randomUUID();
        UUID expectedDefendantId = UUID.randomUUID();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN("test-case-urn-a").build())
                                        .defendants(List.of(
                                                DefendantEntry.builder().id(expectedDefendantId).masterDefendantId(masterDefendantId).build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);

        List<UUID> result = hearingService.resolveDefendantIds(hearingId, masterDefendantId, "test-case-urn-a");

        assertThat(result).containsExactly(expectedDefendantId);
    }

    @Test
    void resolveDefendantIds_should_returnAllMatches_whenSameMasterDefendantIdLinkedAcrossMultipleCases() {
        UUID masterDefendantId = UUID.randomUUID();
        UUID defendantIdInCaseA = UUID.randomUUID();
        UUID defendantIdInCaseB = UUID.randomUUID();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN("test-case-urn-a").build())
                                        .defendants(List.of(
                                                DefendantEntry.builder().id(defendantIdInCaseA).masterDefendantId(masterDefendantId).build()
                                        ))
                                        .build(),
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN("test-case-urn-b").build())
                                        .defendants(List.of(
                                                DefendantEntry.builder().id(defendantIdInCaseB).masterDefendantId(masterDefendantId).build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);

        List<UUID> result = hearingService.resolveDefendantIds(hearingId, masterDefendantId, null);

        assertThat(result).containsExactlyInAnyOrder(defendantIdInCaseA, defendantIdInCaseB);
    }

    @Test
    void resolveDefendantIds_should_ignoreOtherCases_whenCaseUrnGiven() {
        UUID masterDefendantId = UUID.randomUUID();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN("test-case-urn-b").build())
                                        .defendants(List.of(
                                                DefendantEntry.builder().id(UUID.randomUUID()).masterDefendantId(masterDefendantId).build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);

        List<UUID> result = hearingService.resolveDefendantIds(hearingId, masterDefendantId, "test-case-urn-a");

        assertThat(result).isEmpty();
    }

    @Test
    void resolveDefendantIds_should_matchAcrossCases_whenCaseUrnNotGiven() {
        UUID masterDefendantId = UUID.randomUUID();
        UUID expectedDefendantId = UUID.randomUUID();
        HearingResponse hearingResponse = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .prosecutionCases(List.of(
                                ProsecutionCase.builder()
                                        .prosecutionCaseIdentifier(ProsecutionCaseIdentifier.builder().caseURN("test-case-urn-b").build())
                                        .defendants(List.of(
                                                DefendantEntry.builder().id(expectedDefendantId).masterDefendantId(masterDefendantId).build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();
        when(hearingClient.getHearing(hearingId)).thenReturn(hearingResponse);

        List<UUID> result = hearingService.resolveDefendantIds(hearingId, masterDefendantId, null);

        assertThat(result).containsExactly(expectedDefendantId);
    }

    @Test
    void resolveDefendantIds_should_returnEmpty_whenNoDefendantMatches() {
        when(hearingClient.getHearing(hearingId)).thenReturn(HearingResponse.builder()
                .hearing(HearingDetail.builder().build())
                .build());

        List<UUID> result = hearingService.resolveDefendantIds(hearingId, UUID.randomUUID(), null);

        assertThat(result).isEmpty();
    }
}
