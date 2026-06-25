package uk.gov.hmcts.cp.clients;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.cp.config.AppPropertiesBackend;
import uk.gov.hmcts.cp.domain.CaseMapperResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseUrnMapperClientTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private AppPropertiesBackend appProperties;

    @InjectMocks
    private CaseUrnMapperClient caseUrnMapperClient;

    private final String caseUrn = "ABCD1234567";
    private final UUID caseId = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Test
    void getCaseId_should_returnCaseId_whenResponseIsSuccessful() {
        when(appProperties.getCaseMapperUrl()).thenReturn("http://mock-server");
        when(appProperties.getCaseMapperPath()).thenReturn("/urnmapper");
        CaseMapperResponse response = CaseMapperResponse.builder()
                .caseId(caseId)
                .build();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CaseMapperResponse.class)
        )).thenReturn(ResponseEntity.ok(response));

        UUID result = caseUrnMapperClient.getCaseId(caseUrn);

        assertEquals(caseId, result);
    }
}
