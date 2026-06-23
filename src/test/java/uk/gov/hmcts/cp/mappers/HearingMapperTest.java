package uk.gov.hmcts.cp.mappers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse.HearingSummary;
import uk.gov.hmcts.cp.openapi.model.HearingSummaryView;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;
import uk.gov.hmcts.cp.openapi.model.NextAppearance;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HearingMapperTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-23T00:00:00Z"), ZoneOffset.UTC);

    private final HearingMapper hearingMapper = new HearingMapper(FIXED_CLOCK);

    @Test
    void mapToHearingTimelineView_should_returnEmptyTimeline_whenResponseIsNull() {
        HearingTimelineView view = hearingMapper.mapToHearingTimelineView(null);

        assertThat(view.getHearingSummaries()).isEmpty();
        assertThat(view.getNextAppearance()).isNull();
    }

    @Test
    void mapToHearingTimelineView_should_returnEmptyTimeline_whenHearingSummariesIsNull() {
        HearingTimelineResponse response = HearingTimelineResponse.builder().build();

        HearingTimelineView view = hearingMapper.mapToHearingTimelineView(response);

        assertThat(view.getHearingSummaries()).isEmpty();
        assertThat(view.getNextAppearance()).isNull();
    }

    @Test
    void mapToHearingTimelineView_should_sortHearingsChronologicallyAndMapFields() {
        UUID laterId = UUID.randomUUID();
        UUID earlierId = UUID.randomUUID();
        HearingTimelineResponse response = HearingTimelineResponse.builder()
                .hearingSummaries(List.of(
                        HearingSummary.builder()
                                .hearingId(laterId)
                                .hearingDate("2026-06-30")
                                .hearingType("Trial")
                                .courtHouse("Birmingham Crown Court")
                                .courtRoom("Court room 4")
                                .hearingTime("09:30")
                                .startTime("09:45")
                                .outcome("Adjourned")
                                .build(),
                        HearingSummary.builder()
                                .hearingId(earlierId)
                                .hearingDate("2026-05-14")
                                .hearingType("First hearing")
                                .courtHouse("Birmingham Magistrates' Court")
                                .courtRoom("Court room 1")
                                .hearingTime("10:00")
                                .startTime("10:15")
                                .outcome("Sent to Crown Court")
                                .build()
                ))
                .build();

        HearingTimelineView view = hearingMapper.mapToHearingTimelineView(response);

        List<HearingSummaryView> summaries = view.getHearingSummaries();
        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).getId()).isEqualTo(earlierId);
        assertThat(summaries.get(0).getCourtHouse()).isEqualTo("Birmingham Magistrates' Court");
        assertThat(summaries.get(1).getId()).isEqualTo(laterId);
    }

    @Test
    void mapToHearingTimelineView_should_includeNextAppearance_whenFutureHearingExists() {
        UUID futureId = UUID.randomUUID();
        HearingTimelineResponse response = HearingTimelineResponse.builder()
                .hearingSummaries(List.of(
                        HearingSummary.builder()
                                .hearingId(UUID.randomUUID())
                                .hearingDate("2026-05-14")
                                .build(),
                        HearingSummary.builder()
                                .hearingId(futureId)
                                .hearingDate("2026-06-30")
                                .hearingType("Trial")
                                .courtHouse("Birmingham Crown Court")
                                .hearingTime("09:30")
                                .build()
                ))
                .build();

        HearingTimelineView view = hearingMapper.mapToHearingTimelineView(response);

        NextAppearance nextAppearance = view.getNextAppearance();
        assertThat(nextAppearance).isNotNull();
        assertThat(nextAppearance.getHearingId()).isEqualTo(futureId);
        assertThat(nextAppearance.getId()).isEqualTo("NA_" + futureId);
        assertThat(nextAppearance.getCourt()).isEqualTo("Birmingham Crown Court");
    }

    @Test
    void mapToHearingTimelineView_should_returnNullNextAppearance_whenAllHearingsAreInThePast() {
        HearingTimelineResponse response = HearingTimelineResponse.builder()
                .hearingSummaries(List.of(
                        HearingSummary.builder()
                                .hearingId(UUID.randomUUID())
                                .hearingDate("2026-05-14")
                                .build()
                ))
                .build();

        HearingTimelineView view = hearingMapper.mapToHearingTimelineView(response);

        assertThat(view.getNextAppearance()).isNull();
    }

    @Test
    void mapToHearingTimelineView_should_skipUnparseableHearingDate() {
        HearingTimelineResponse response = HearingTimelineResponse.builder()
                .hearingSummaries(List.of(
                        HearingSummary.builder()
                                .hearingId(UUID.randomUUID())
                                .hearingDate("not-a-date")
                                .hearingType("Trial")
                                .build()
                ))
                .build();

        HearingTimelineView view = hearingMapper.mapToHearingTimelineView(response);

        assertThat(view.getHearingSummaries()).hasSize(1);
        assertThat(view.getHearingSummaries().get(0).getDate()).isNull();
        assertThat(view.getNextAppearance()).isNull();
    }
}