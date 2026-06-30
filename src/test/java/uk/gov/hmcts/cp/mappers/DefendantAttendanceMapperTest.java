package uk.gov.hmcts.cp.mappers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.domain.HearingResponse;
import uk.gov.hmcts.cp.domain.HearingResponse.AttendanceDayEntry;
import uk.gov.hmcts.cp.domain.HearingResponse.DefendantAttendanceEntry;
import uk.gov.hmcts.cp.domain.HearingResponse.HearingDetail;
import uk.gov.hmcts.cp.openapi.model.AttendanceDay;
import uk.gov.hmcts.cp.openapi.model.DefendantAttendanceView;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantAttendanceMapperTest {

    private final DefendantAttendanceMapper mapper = new DefendantAttendanceMapper();

    private final UUID hearingId = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private final UUID defendantId1 = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private final UUID defendantId2 = UUID.fromString("00000000-0000-0000-0000-000000000023");

    @Test
    void mapToDefendantAttendanceView_should_returnEmptyDefendants_whenResponseIsNull() {
        DefendantAttendanceView view = mapper.mapToDefendantAttendanceView(hearingId, null);

        assertThat(view.getId()).isEqualTo(hearingId);
        assertThat(view.getDefendants()).isEmpty();
    }

    @Test
    void mapToDefendantAttendanceView_should_returnEmptyDefendants_whenDefendantAttendanceIsNull() {
        HearingResponse response = HearingResponse.builder()
                .hearing(HearingDetail.builder().build())
                .build();

        DefendantAttendanceView view = mapper.mapToDefendantAttendanceView(hearingId, response);

        assertThat(view.getDefendants()).isEmpty();
    }

    @Test
    void mapToDefendantAttendanceView_should_mapMultipleDefendantsAndDays() {
        HearingResponse response = HearingResponse.builder()
                .hearing(HearingDetail.builder()
                        .defendantAttendance(List.of(
                                DefendantAttendanceEntry.builder()
                                        .defendantId(defendantId1)
                                        .attendanceDays(List.of(
                                                AttendanceDayEntry.builder().day("2026-06-23").attendanceType("IN_PERSON").build(),
                                                AttendanceDayEntry.builder().day("2026-06-24").attendanceType("VIDEO_LINK").build()
                                        ))
                                        .build(),
                                DefendantAttendanceEntry.builder()
                                        .defendantId(defendantId2)
                                        .attendanceDays(List.of(
                                                AttendanceDayEntry.builder().day("2026-06-23").attendanceType("DID_NOT_APPEAR").build()
                                        ))
                                        .build()
                        ))
                        .build())
                .build();

        DefendantAttendanceView view = mapper.mapToDefendantAttendanceView(hearingId, response);

        assertThat(view.getId()).isEqualTo(hearingId);
        assertThat(view.getDefendants()).hasSize(2);
        assertThat(view.getDefendants().get(0).getId()).isEqualTo(defendantId1);
        assertThat(view.getDefendants().get(0).getAttendanceDays()).containsExactly(
                AttendanceDay.builder().day(LocalDate.of(2026, 6, 23)).type(AttendanceDay.TypeEnum.IN_PERSON).build(),
                AttendanceDay.builder().day(LocalDate.of(2026, 6, 24)).type(AttendanceDay.TypeEnum.VIDEO_LINK).build()
        );
        assertThat(view.getDefendants().get(1).getId()).isEqualTo(defendantId2);
        assertThat(view.getDefendants().get(1).getAttendanceDays()).containsExactly(
                AttendanceDay.builder().day(LocalDate.of(2026, 6, 23)).type(AttendanceDay.TypeEnum.DID_NOT_APPEAR).build()
        );
    }
}