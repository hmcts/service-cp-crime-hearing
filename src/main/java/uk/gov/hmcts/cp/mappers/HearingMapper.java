package uk.gov.hmcts.cp.mappers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse;
import uk.gov.hmcts.cp.domain.HearingTimelineResponse.HearingSummary;
import uk.gov.hmcts.cp.openapi.model.HearingSummaryView;
import uk.gov.hmcts.cp.openapi.model.HearingTimelineView;
import uk.gov.hmcts.cp.openapi.model.NextAppearance;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class HearingMapper {

    private final Clock clock;

    public HearingTimelineView mapToHearingTimelineView(final HearingTimelineResponse response) {
        final List<HearingSummary> summaries = Optional.ofNullable(response)
                .map(HearingTimelineResponse::getHearingSummaries)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .toList();

        final List<HearingSummaryView> sortedViews = summaries.stream()
                .sorted(Comparator.comparing(this::parseDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toView)
                .toList();

        return HearingTimelineView.builder()
                .hearingSummaries(sortedViews)
                .nextAppearance(resolveNextAppearance(summaries))
                .build();
    }

    private HearingSummaryView toView(final HearingSummary summary) {
        return HearingSummaryView.builder()
                .id(summary.getHearingId())
                .date(parseDate(summary))
                .type(summary.getHearingType())
                .courtHouse(summary.getCourtHouse())
                .courtRoom(summary.getCourtRoom())
                .time(summary.getHearingTime())
                .startTime(summary.getStartTime())
                .outcome(summary.getOutcome())
                .build();
    }

    private NextAppearance resolveNextAppearance(final List<HearingSummary> summaries) {
        final LocalDate today = LocalDate.now(clock);
        return summaries.stream()
                .filter(s -> parseDate(s) != null && !parseDate(s).isBefore(today))
                .min(Comparator.comparing(this::parseDate))
                .map(s -> NextAppearance.builder()
                        .id("NA_" + s.getHearingId())
                        .court(s.getCourtHouse())
                        .date(parseDate(s))
                        .time(s.getHearingTime() != null ? s.getHearingTime() : s.getStartTime())
                        .type(s.getHearingType())
                        .hearingId(s.getHearingId())
                        .build())
                .orElse(null);
    }

    private LocalDate parseDate(final HearingSummary summary) {
        LocalDate date = null;
        if (summary != null && summary.getHearingDate() != null) {
            try {
                date = LocalDate.parse(summary.getHearingDate());
            } catch (DateTimeParseException e) {
                log.warn(
                        "Unparseable hearingDate '{}' for hearingId {}",
                        Encode.forJava(summary.getHearingDate()),
                        Encode.forJava(String.valueOf(summary.getHearingId()))
                );
            }
        }
        return date;
    }
}