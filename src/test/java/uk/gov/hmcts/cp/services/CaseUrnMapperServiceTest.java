package uk.gov.hmcts.cp.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.cp.clients.CaseUrnMapperClient;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseUrnMapperServiceTest {

    @Mock
    private CaseUrnMapperClient caseUrnMapperClient;

    @InjectMocks
    private CaseUrnMapperService caseUrnMapperService;

    private final UUID caseId = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private final String caseUrn = "ABCD1234567";

    @Test
    void getCaseId_should_returnCaseId_whenClientResolvesSuccessfully() {
        when(caseUrnMapperClient.getCaseId(caseUrn)).thenReturn(caseId);

        UUID result = caseUrnMapperService.getCaseId(caseUrn);

        assertEquals(caseId, result);
        verify(caseUrnMapperClient).getCaseId(caseUrn);
    }
}
