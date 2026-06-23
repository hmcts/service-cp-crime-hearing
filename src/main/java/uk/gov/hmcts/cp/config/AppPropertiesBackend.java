package uk.gov.hmcts.cp.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Getter
public class AppPropertiesBackend {

    private final String hearingUrl;
    private final String hearingPath;
    private final String hearingCjscppuid;
    private final String caseMapperUrl;
    private final String caseMapperPath;

    public AppPropertiesBackend(
            @Value("${hearing-client.url}") final String hearingUrl,
            @Value("${hearing-client.path}") final String hearingPath,
            @Value("${hearing-client.cjscppuid:}") final String hearingCjscppuid,
            @Value("${case-mapper-client.url}") final String caseMapperUrl,
            @Value("${case-mapper-client.path}") final String caseMapperPath) {
        this.hearingUrl = hearingUrl;
        this.hearingPath = hearingPath;
        this.hearingCjscppuid = hearingCjscppuid;
        this.caseMapperUrl = caseMapperUrl;
        this.caseMapperPath = caseMapperPath;
    }
}
