Feature: check getCaseTimeline for smoke testing

  Run only - reads caseUrn from case-config/<env>.json (written by Setup on dev/sit, or
  maintained by support/third-line on prp/prd) and verifies the timeline is retrievable. Runs on
  every tier - Run doesn't know or care which of the two produced the file it reads (design §4a).

  Background:
    * def smokeUtils = read('smoke-utils.js')()
    * def caseConfig = smokeUtils.readCaseConfig(environment)
    * assert caseConfig.environment == environment
    * def caseUrn = caseConfig.caseUrn

  Scenario: verify getCaseTimeline returns the configured case

    # When - GET the timeline through APIM/WAF (Entra bearer + Ocp-Apim-Subscription-Key)
    Given url tokenUrl
    And header Content-Type = 'application/x-www-form-urlencoded'
    And form field grant_type = 'client_credentials'
    And form field client_id = entraClientId
    And form field client_secret = entraClientSecret
    And form field scope = entraScope
    When method post
    Then status 200
    * def accessToken = response.access_token

    Given url serviceBaseUrl + '/hearings/cases/' + caseUrn + '/timeline'
    And header Authorization = 'Bearer ' + accessToken
    And header Ocp-Apim-Subscription-Key = apimSubscriptionKey
    When method get
    Then status 200

    # Then - assert & report result
    And match response.hearingSummaries != '#[0]'
    * karate.log('Hearing timeline verified for caseUrn:', caseUrn, 'hearingSummaryCount:', response.hearingSummaries.length, 'nextAppearance:', response.nextAppearance)