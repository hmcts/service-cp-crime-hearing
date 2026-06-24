package uk.gov.hmcts.cp.mappers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.domain.HearingResponse.AttendanceDayEntry;
import uk.gov.hmcts.cp.domain.HearingResponse.DefendantAttendanceEntry;
import uk.gov.hmcts.cp.openapi.model.AttendanceDay;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendance;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendanceView;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DefendantAttendanceMapper {

    public DefendantAttendanceView mapToDefendantAttendanceView(final UUID hearingId, final HearingResponse response) {
        final List<DefendantAttendanceEntry> entries = Optional.ofNullable(response)
                .map(HearingResponse::getHearing)
                .map(HearingResponse.HearingDetail::getDefendantAttendance)
                .orElse(Collections.emptyList());

        return DefendantAttendanceView.builder()
                .id(hearingId)
                .defendants(entries.stream().map(this::toDefendantAttendance).toList())
                .build();
    }

    private DefendantAttendance toDefendantAttendance(final DefendantAttendanceEntry entry) {
        final List<AttendanceDay> days = Optional.ofNullable(entry.getAttendanceDays())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toAttendanceDay)
                .toList();

        return DefendantAttendance.builder()
                .id(entry.getDefendantId())
                .attendanceDays(days)
                .build();
    }

    private AttendanceDay toAttendanceDay(final AttendanceDayEntry entry) {
        return AttendanceDay.builder()
                .day(LocalDate.parse(entry.getDay()))
                .type(AttendanceDay.TypeEnum.fromValue(entry.getAttendanceType()))
                .build();
    }
}