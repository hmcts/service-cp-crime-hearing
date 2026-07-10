Feature: create a CP case for smoke testing

  Setup only - creates a case via SPI-IN and records it in case-config/<env>.json for
  run-check-timeline.feature to read. Dev/sit only - prp/prd cannot create cases and have no
  Setup step (design doc §4/§4a).

  Background:
    * def smokeUtils = read('smoke-utils.js')()
    * def caseUrn = smokeUtils.generateUrn()

  Scenario: submit SPI-IN case creation and record the caseUrn

    * def requestId = java.util.UUID.randomUUID() + ''
    * def hearingDate = smokeUtils.hearingDatePlusDays(4)
    * def soapTemplate = karate.readAsString('classpath:spi-in-minimal.xml')
    * def soapEnvelope = soapTemplate.replace('REQUEST_ID', requestId).replace('NEW_URN', caseUrn).replace('TODAY+4', hearingDate).replace('ADDRESS_LINE1', '1 Smoke Test Street')

    Given url backendBaseUrl + '/stagingprosecutorsspi-service/CJSEService'
    And header Content-Type = 'application/soap+xml;charset=UTF-8'
    And header CJSCPPUID = cjscppuid
    And request soapEnvelope
    When method post
    Then status 200
    * def responseText = new java.lang.String(responseBytes, 'UTF-8')
    And match responseText contains '<ResponseCode>1</ResponseCode>'

    * smokeUtils.writeCaseConfig(environment, caseUrn)
    * karate.log('case-config written for environment:', environment, 'caseUrn:', caseUrn)