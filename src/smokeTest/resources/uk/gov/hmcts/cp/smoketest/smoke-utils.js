function fn() {
  var Files = Java.type('java.nio.file.Files');
  var Paths = Java.type('java.nio.file.Paths');
  var StandardCharsets = Java.type('java.nio.charset.StandardCharsets');

  // Dev/sit write here at pipeline runtime - build-output-relative, gitignored, never touches
  // the source tree. Prp/prd have no Setup step; their case-config is checked into
  // src/smokeTest/resources/case-config/<env>.json instead and read via the classpath fallback
  // below. Run doesn't know or care which produced the file it reads (design doc §4a).
  var CASE_CONFIG_OVERRIDE_DIR = Paths.get('build', 'smoke-test-config');

  function todayIso() {
    var d = new Date();
    var yyyy = d.getFullYear();
    var mm = ('0' + (d.getMonth() + 1)).slice(-2);
    var dd = ('0' + d.getDate()).slice(-2);
    return yyyy + '-' + mm + '-' + dd;
  }

  return {
    generateUrn: function() {
      var unit = Math.floor(Math.random() * (99 - 10 + 1)) + 10;
      var number = Math.floor(Math.random() * (99999 - 10000 + 1)) + 10000;
      var yy = ('' + new Date().getFullYear()).slice(-2);
      return unit + 'GD' + number + yy;
    },
    hearingDatePlusDays: function(days) {
      var d = new Date();
      d.setDate(d.getDate() + days);
      var yyyy = d.getFullYear();
      var mm = ('0' + (d.getMonth() + 1)).slice(-2);
      var dd = ('0' + d.getDate()).slice(-2);
      return yyyy + '-' + mm + '-' + dd;
    },
    writeCaseConfig: function(environment, caseUrn) {
      Files.createDirectories(CASE_CONFIG_OVERRIDE_DIR);
      var config = {
        environment: environment,
        caseUrn: caseUrn,
        source: 'automated-setup',
        lastConfirmedDate: todayIso(),
        notes: ''
      };
      var path = CASE_CONFIG_OVERRIDE_DIR.resolve(environment + '.json');
      Files.write(path, JSON.stringify(config, null, 2).getBytes(StandardCharsets.UTF_8));
    },
    readCaseConfig: function(environment) {
      var overridePath = CASE_CONFIG_OVERRIDE_DIR.resolve(environment + '.json');
      var text;
      if (Files.exists(overridePath)) {
        text = new java.lang.String(Files.readAllBytes(overridePath), StandardCharsets.UTF_8);
      } else {
        text = karate.readAsString('classpath:case-config/' + environment + '.json');
      }
      return JSON.parse(text);
    }
  };
}