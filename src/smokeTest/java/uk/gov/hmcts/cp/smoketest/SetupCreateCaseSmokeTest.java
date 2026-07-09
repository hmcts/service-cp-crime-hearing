package uk.gov.hmcts.cp.smoketest;

import com.intuit.karate.junit5.Karate;

class SetupCreateCaseSmokeTest {

    @Karate.Test
    Karate testSetupCreateCase() {
        return Karate.run("setup-create-case").relativeTo(getClass());
    }
}