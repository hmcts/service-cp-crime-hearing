package uk.gov.hmcts.cp.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class HearingTimelineResponse {

    private List<HearingSummary> hearingSummaries;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    public static class HearingSummary {
        private UUID hearingId;
        private String hearingDate;
        private String hearingType;
        private String courtHouse;
        private String courtRoom;
        private String hearingTime;
        private String startTime;
        private String outcome;
    }
}