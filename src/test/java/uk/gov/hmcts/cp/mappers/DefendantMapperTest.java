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

    private static final UUID MASTER_DEFENDANT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DEFENDANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");
    private static final UUID OFFENCE_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    private final DefendantMapper mapper = new DefendantMapper();

    @Test
    void mapToDefendantViews_should_returnEmptyList_whenEntriesIsNull() {
        List<DefendantView> views = mapper.mapToDefendantViews(null);

        assertThat(views).isEmpty();
    }

    @Test
    void mapToDefendantViews_should_mapIdAndMasterDefendantId() {
        DefendantEntry entry = DefendantEntry.builder().id(DEFENDANT_ID).masterDefendantId(MASTER_DEFENDANT_ID).build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views).hasSize(1);
        assertThat(views.get(0).getId()).isEqualTo(DEFENDANT_ID);
        assertThat(views.get(0).getMasterDefendantId()).isEqualTo(MASTER_DEFENDANT_ID);
    }

    @Test
    void mapToDefendantViews_should_mapNameFromPersonDefendant() {
        DefendantEntry entry = DefendantEntry.builder()
                .id(DEFENDANT_ID)
                .personDefendant(PersonDefendant.builder()
                        .personDetails(PersonDetails.builder().firstName("Alessia").lastName("Mitchell").build())
                        .build())
                .build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getName()).isEqualTo("Alessia Mitchell");
    }

    @Test
    void mapToDefendantViews_should_returnNullName_whenPersonDefendantIsAbsent() {
        DefendantEntry entry = DefendantEntry.builder().id(DEFENDANT_ID).build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getName()).isNull();
    }

    @Test
    void mapToDefendantViews_should_mapOffenceCodeAndTitleAndPleaValueAsStatus() {
        DefendantEntry entry = DefendantEntry.builder()
                .id(DEFENDANT_ID)
                .offences(List.of(
                        OffenceEntry.builder()
                                .id(OFFENCE_ID)
                                .offenceCode("OF61102C")
                                .offenceTitle("Conspire to assault a person thereby occasioning them actual bodily harm")
                                .plea(PleaEntry.builder().pleaValue("GUILTY").build())
                                .build()
                ))
                .build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getOffences()).hasSize(1);
        assertThat(views.get(0).getOffences().get(0).getId()).isEqualTo(OFFENCE_ID);
        assertThat(views.get(0).getOffences().get(0).getCode()).isEqualTo("OF61102C");
        assertThat(views.get(0).getOffences().get(0).getTitle()).isEqualTo("Conspire to assault a person thereby occasioning them actual bodily harm");
        assertThat(views.get(0).getOffences().get(0).getStatus()).isEqualTo("GUILTY");
    }

    @Test
    void mapToDefendantViews_should_defaultOffenceStatusToAwaitingPlea_whenNoPleaRecorded() {
        DefendantEntry entry = DefendantEntry.builder()
                .id(DEFENDANT_ID)
                .offences(List.of(OffenceEntry.builder().id(OFFENCE_ID).build()))
                .build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getOffences().get(0).getStatus()).isEqualTo("Awaiting plea");
    }

    @Test
    void mapToDefendantViews_should_returnEmptyOffences_whenNoneRecorded() {
        DefendantEntry entry = DefendantEntry.builder().id(DEFENDANT_ID).build();

        List<DefendantView> views = mapper.mapToDefendantViews(List.of(entry));

        assertThat(views.get(0).getOffences()).isEmpty();
    }

    @Test
    void mapToDefendantView_should_mapSingleEntry() {
        DefendantEntry entry = DefendantEntry.builder()
                .id(DEFENDANT_ID)
                .masterDefendantId(MASTER_DEFENDANT_ID)
                .personDefendant(PersonDefendant.builder()
                        .personDetails(PersonDetails.builder().firstName("John").lastName("Doe").build())
                        .build())
                .offences(List.of(OffenceEntry.builder()
                        .id(OFFENCE_ID)
                        .offenceCode("TH68001")
                        .offenceTitle("Theft from a shop")
                        .plea(PleaEntry.builder().pleaValue("GUILTY").build())
                        .build()))
                .build();

        DefendantView view = mapper.mapToDefendantView(entry);

        assertThat(view.getId()).isEqualTo(DEFENDANT_ID);
        assertThat(view.getMasterDefendantId()).isEqualTo(MASTER_DEFENDANT_ID);
        assertThat(view.getName()).isEqualTo("John Doe");
        assertThat(view.getOffences()).hasSize(1);
        assertThat(view.getOffences().get(0).getCode()).isEqualTo("TH68001");
        assertThat(view.getOffences().get(0).getStatus()).isEqualTo("GUILTY");
    }
}