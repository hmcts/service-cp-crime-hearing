package uk.gov.hmcts.cp.smoketest;

import com.intuit.karate.junit5.Karate;

class RunCheckTimelineSmokeTest {

    @Karate.Test
    Karate testRunCheckTimeline() {
        return Karate.run("run-check-timeline").relativeTo(getClass());
    }
}