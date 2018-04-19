import sbt.Tests.{Group, SubProcess}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.SbtAutoBuildPlugin

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-play-25" % "1.5.0",
  "uk.gov.hmrc" %% "govuk-template" % "5.17.0",
  "uk.gov.hmrc" %% "play-ui" % "7.13.0",
  "uk.gov.hmrc" %% "auth-client" % "2.5.0",
  "uk.gov.hmrc" %% "play-partials" % "6.1.0",
  "uk.gov.hmrc" %% "agent-kenshoo-monitoring" % "2.4.0",
  "uk.gov.hmrc" %% "agent-mtd-identifiers" % "0.10.0",
  "uk.gov.hmrc" %% "domain" % "5.1.0"
)

def testDeps(scope: String) = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.0.0" % scope,
  "org.scalatest" %% "scalatest" % "3.0.4" % scope,
  "org.mockito" % "mockito-core" % "2.13.0" % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
  "com.github.tomakehurst" % "wiremock" % "2.13.0" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "org.jsoup" % "jsoup" % "1.11.2" % scope
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-mapping-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.11.11",
    PlayKeys.playDefaultPort := 9438,
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.bintrayRepo("hmrc", "release-candidates"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    ),
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    publishingSettings,
    scoverageSettings,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    routesImport ++= Seq("uk.gov.hmrc.agentmappingfrontend.controllers.UrlBinders._")
  )
  .configs(IntegrationTest)
  .settings(
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false,
    testGrouping in IntegrationTest := oneForkedJvmPerTest((definedTests in IntegrationTest).value)
  )
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) = {
  tests.map { test =>
    new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq(s"-Dtest.name=${test.name}"))))
  }
}