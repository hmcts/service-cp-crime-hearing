package uk.gov.hmcts.cp.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.clients.HearingClient;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;
import uk.gov.hmcts.cp.mappers.HearingMapper;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;

import java.util.UUID;

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

    @InjectMocks
    private HearingService hearingService;

    private final String caseUrn = "test-case-urn";
    private final UUID caseId = UUID.fromString("7a2e94c4-38af-43dd-906b-40d632d159b0");

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
}
