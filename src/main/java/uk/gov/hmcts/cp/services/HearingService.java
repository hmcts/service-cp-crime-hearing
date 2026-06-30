package uk.gov.hmcts.cp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.clients.HearingClient;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;
import uk.gov.hmcts.cp.mappers.DefendantAttendanceMapper;
import uk.gov.hmcts.cp.mappers.DefendantMapper;
import uk.gov.hmcts.cp.mappers.HearingMapper;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendanceView;
import uk.gov.hmcts.cp.openapi.model.DefendantView;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HearingService {

    private final CaseUrnMapperService caseUrnMapperService;
    private final HearingClient hearingClient;
    private final HearingMapper hearingMapper;
    private final DefendantAttendanceMapper defendantAttendanceMapper;
    private final DefendantMapper defendantMapper;

    public HearingTimelineView getCaseTimeline(final String caseUrn) {
        final UUID caseId = caseUrnMapperService.getCaseId(caseUrn);
        final HearingTimelineResponse timelineResponse = hearingClient.getTimeline(caseId);
        return hearingMapper.mapToHearingTimelineView(timelineResponse);
    }

    public DefendantAttendanceView getDefendantAttendance(final UUID hearingId) {
        final HearingResponse hearingResponse = hearingClient.getHearing(hearingId);
        return defendantAttendanceMapper.mapToDefendantAttendanceView(hearingId, hearingResponse);
    }

    public List<DefendantView> getDefendants(final UUID hearingId, final String caseURN, final UUID masterDefendantId) {
        final HearingResponse hearingResponse = hearingClient.getHearing(hearingId);
        final List<HearingResponse.ProsecutionCase> prosecutionCases = Optional.ofNullable(hearingResponse)
                .map(HearingResponse::getHearing)
                .map(HearingResponse.HearingDetail::getProsecutionCases)
                .orElse(Collections.emptyList());

        final List<HearingResponse.DefendantEntry> defendants = prosecutionCases.stream()
                .filter(pc -> matchesCaseUrn(pc, caseURN))
                .flatMap(pc -> Optional.ofNullable(pc.getDefendants()).orElse(Collections.emptyList()).stream())
                .filter(d -> masterDefendantId == null || masterDefendantId.equals(d.getMasterDefendantId()))
                .toList();

        return defendantMapper.mapToDefendantViews(defendants);
    }

    private boolean matchesCaseUrn(final HearingResponse.ProsecutionCase prosecutionCase, final String caseUrn) {
        return Optional.ofNullable(prosecutionCase.getProsecutionCaseIdentifier())
                .map(HearingResponse.ProsecutionCaseIdentifier::getCaseURN)
                .map(urn -> Objects.equals(urn, caseUrn))
                .orElse(false);
    }
}
