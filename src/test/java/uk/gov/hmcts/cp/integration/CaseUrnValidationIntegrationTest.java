package uk.gov.hmcts.cp.integration;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaseUrnValidationIntegrationTest extends IntegrationTestBase {

    private static final String EXPECTED_MESSAGE = "Case urn must be between 1 and 30 alphanumerics";
    private static final String HEARING_ID = "00000000-0000-0000-0000-000000000011";

    @Test
    void getCaseTimeline_should_return400_whenCaseUrnNonAlphanumeric() throws Exception {
        mockMvc.perform(get("/hearings/cases/{caseURN}/timeline", "bad-urn!"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(EXPECTED_MESSAGE));
    }

    @Test
    void getCaseTimeline_should_return400_whenCaseUrnTooLong() throws Exception {
        mockMvc.perform(get("/hearings/cases/{caseURN}/timeline", "A".repeat(31)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(EXPECTED_MESSAGE));
    }

    @Test
    void getCaseTimeline_should_return404_whenCaseUrnEmpty() throws Exception {
        mockMvc.perform(get("/hearings/cases/{caseURN}/timeline", ""))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void getDefendants_should_return400_whenCaseUrnNonAlphanumeric() throws Exception {
        mockMvc.perform(get("/hearings/{hearingId}/cases/{caseURN}/defendants", HEARING_ID, "bad-urn!"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(EXPECTED_MESSAGE));
    }

    @Test
    void getDefendants_should_return400_whenCaseUrnTooLong() throws Exception {
        mockMvc.perform(get("/hearings/{hearingId}/cases/{caseURN}/defendants", HEARING_ID, "A".repeat(31)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(EXPECTED_MESSAGE));
    }

    @Test
    void getDefendants_should_return404_whenCaseUrnEmpty() throws Exception {
        mockMvc.perform(get("/hearings/{hearingId}/cases/{caseURN}/defendants", HEARING_ID, ""))
                .andDo(print())
                .andExpect(status().isNotFound());
    }
}