package uk.gov.hmcts.cp.mappers;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.cp.domain.HearingResponse.DefendantEntry;
import uk.gov.hmcts.cp.domain.HearingResponse.OffenceEntry;
import uk.gov.hmcts.cp.domain.HearingResponse.PersonDefendant;
import uk.gov.hmcts.cp.domain.HearingResponse.PersonDetails;
import uk.gov.hmcts.cp.domain.HearingResponse.PleaEntry;
import uk.gov.hmcts.cp.openapi.model.DefendantView;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefendantMapperTest {

    private final DefendantMapper mapper = new DefendantMapper();

    @Test
    void mapToDefendantViews_should_returnEmptyList_whenEntriesIsNull() {
        List<DefendantView> views = mapper.mapToDefendantViews(null);

        assertThat(views).isEmpty();
    }

    @Test
    void mapToDefendantViews_should_mapIdAndMasterDefendantId() {
        UUID id = UUID.randomUUID();
        UUID masterDefendantId = UUID.randomUUID();
        DefendantEntry entry = DefendantEntry.builder().id(id).masterDefendantId(masterDefendantId).build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).getId()).isEqualTo(id);
        assertThat(views.get(0).getMasterDefendantId()).isEqualTo(masterDefendantId);
    }

    @Test
    void mapToDefendantViews_should_mapNameFromPersonDefendant() {
        DefendantEntry entry = DefendantEntry.builder()
                .id(UUID.randomUUID())
                .personDefendant(PersonDefendant.builder()
                        .personDetails(PersonDetails.builder().firstName("Alessia").lastName("Mitchell").build())
                        .build())
                .build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getName()).isEqualTo("Alessia Mitchell");
    }

    @Test
    void mapToDefendantViews_should_returnNullName_whenPersonDefendantIsAbsent() {
        DefendantEntry entry = DefendantEntry.builder().id(UUID.randomUUID()).build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getName()).isNull();
    }

    @Test
    void mapToDefendantViews_should_mapOffenceCodeAndTitleAndPleaValueAsStatus() {
        UUID offenceId = UUID.randomUUID();
        DefendantEntry entry = DefendantEntry.builder()
                .id(UUID.randomUUID())
                .offences(List.of(
                        OffenceEntry.builder()
                                .id(offenceId)
                                .offenceCode("OF61102C")
                                .offenceTitle("Conspire to assault a person thereby occasioning them actual bodily harm")
                                .plea(PleaEntry.builder().pleaValue("GUILTY").build())
                                .build()
                ))
                .build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getOffences()).hasSize(1);
        assertThat(views.get(0).getOffences().get(0).getId()).isEqualTo(offenceId);
        assertThat(views.get(0).getOffences().get(0).getCode()).isEqualTo("OF61102C");
        assertThat(views.get(0).getOffences().get(0).getTitle()).isEqualTo("Conspire to assault a person thereby occasioning them actual bodily harm");
        assertThat(views.get(0).getOffences().get(0).getStatus()).isEqualTo("GUILTY");
    }

    @Test
    void mapToDefendantViews_should_defaultOffenceStatusToActive_whenNoPleaRecorded() {
        DefendantEntry entry = DefendantEntry.builder()
                .id(UUID.randomUUID())
                .offences(List.of(OffenceEntry.builder().id(UUID.randomUUID()).build()))
                .build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getOffences().get(0).getStatus()).isEqualTo("Active");
    }

    @Test
    void mapToDefendantViews_should_returnEmptyOffences_whenNoneRecorded() {
        DefendantEntry entry = DefendantEntry.builder().id(UUID.randomUUID()).build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getOffences()).isEmpty();
    }
}