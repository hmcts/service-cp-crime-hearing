package uk.gov.hmcts.cp.mappers;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.cp.domain.HearingResponse.DefendantEntry;
import uk.gov.hmcts.cp.domain.HearingResponse.OffenceEntry;
import uk.gov.hmcts.cp.domain.HearingResponse.PersonDefendant;
import uk.gov.hmcts.cp.domain.HearingResponse.PleaEntry;
import uk.gov.hmcts.cp.openapi.model.DefendantView;
import uk.gov.hmcts.cp.openapi.model.OffenceView;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class DefendantMapper {

    private static final String DEFAULT_OFFENCE_STATUS = "Awaiting plea";

    public List<DefendantView> mapToDefendantViews(final List<DefendantEntry> entries) {
        return Optional.ofNullable(entries)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toDefendantView)
                .toList();
    }

    private DefendantView toDefendantView(final DefendantEntry entry) {
        return DefendantView.builder()
                .id(entry.getId())
                .masterDefendantId(entry.getMasterDefendantId())
                .name(toName(entry.getPersonDefendant()))
                .offences(toOffenceViews(entry.getOffences()))
                .build();
    }

    private String toName(final PersonDefendant personDefendant) {
        return Optional.ofNullable(personDefendant)
                .map(PersonDefendant::getPersonDetails)
                .map(details -> (details.getFirstName() + " " + details.getLastName()).strip())
                .orElse(null);
    }

    private List<OffenceView> toOffenceViews(final List<OffenceEntry> offences) {
        return Optional.ofNullable(offences)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::toOffenceView)
                .toList();
    }

    private OffenceView toOffenceView(final OffenceEntry offence) {
        return OffenceView.builder()
                .id(offence.getId())
                .code(offence.getOffenceCode())
                .title(offence.getOffenceTitle())
                .status(toStatus(offence.getPlea()))
                .build();
    }

    private String toStatus(final PleaEntry plea) {
        final String pleaValue = Optional.ofNullable(plea).map(PleaEntry::getPleaValue).orElse(null);
        // TBD - Conviction date is primarily derived from the offence-level plea.
        // In some cases, CP can also derive the convictied when the offence has the isConvictedResult flag.
        // If a plea is applied and the offence is resulted at a later hearing, the plea submission date is used as the conviction date.
        return (pleaValue == null || pleaValue.isBlank()) ? DEFAULT_OFFENCE_STATUS : pleaValue;
    }
}