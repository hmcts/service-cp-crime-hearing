package uk.gov.hmcts.cp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.clients.HearingClient;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;
import uk.gov.hmcts.cp.mappers.DefendantAttendanceMapper;
import uk.gov.hmcts.cp.mappers.HearingMapper;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendanceView;
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

    public HearingTimelineView getCaseTimeline(final String caseUrn) {
        final UUID caseId = caseUrnMapperService.getCaseId(caseUrn);
        final HearingTimelineResponse timelineResponse = hearingClient.getTimeline(caseId);
        return hearingMapper.mapToHearingTimelineView(timelineResponse);
    }

    public DefendantAttendanceView getDefendantAttendance(final UUID hearingId) {
        final HearingResponse hearingResponse = hearingClient.getHearing(hearingId);
        return defendantAttendanceMapper.mapToDefendantAttendanceView(hearingId, hearingResponse);
    }

    public Optional<UUID> resolveDefendantId(final UUID hearingId, final UUID masterDefendantId, final String caseUrn) {
        final HearingResponse hearingResponse = hearingClient.getHearing(hearingId);
        final List<HearingResponse.ProsecutionCase> prosecutionCases = Optional.ofNullable(hearingResponse)
                .map(HearingResponse::getHearing)
                .map(HearingResponse.HearingDetail::getProsecutionCases)
                .orElse(Collections.emptyList());

        return prosecutionCases.stream()
                .filter(pc -> caseUrn == null || matchesCaseUrn(pc, caseUrn))
                .flatMap(pc -> Optional.ofNullable(pc.getDefendants()).orElse(Collections.emptyList()).stream())
                .filter(d -> masterDefendantId.equals(d.getMasterDefendantId()))
                .map(HearingResponse.DefendantEntry::getId)
                .findFirst();
    }

    private boolean matchesCaseUrn(final HearingResponse.ProsecutionCase prosecutionCase, final String caseUrn) {
        return Optional.ofNullable(prosecutionCase.getProsecutionCaseIdentifier())
                .map(HearingResponse.ProsecutionCaseIdentifier::getCaseURN)
                .map(urn -> Objects.equals(urn, caseUrn))
                .orElse(false);
    }
}
