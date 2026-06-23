package uk.gov.hmcts.cp.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.cp.clients.HearingClient;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;
import uk.gov.hmcts.cp.mappers.HearingMapper;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HearingService {

    private final CaseUrnMapperService caseUrnMapperService;
    private final HearingClient hearingClient;
    private final HearingMapper hearingMapper;

    public HearingTimelineView getCaseTimeline(final String caseUrn) {
        final UUID caseId = caseUrnMapperService.getCaseId(caseUrn);
        final HearingTimelineResponse timelineResponse = hearingClient.getTimeline(caseId);
        return hearingMapper.mapToHearingTimelineView(timelineResponse);
    }
}
