import sbt.Keys.resolvers
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimum := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

lazy val compileDeps = Seq(
  ws,
  "uk.gov.hmrc"       %% "bootstrap-frontend-play-28" % "5.16.0",
  "com.typesafe.play" %% "play-json-joda"             % "2.9.2",
  "uk.gov.hmrc"       %% "play-frontend-hmrc"         % "1.22.0-play-28",
  "uk.gov.hmrc"       %% "play-partials"              % "8.2.0-play-28",
  "uk.gov.hmrc"       %% "simple-reactivemongo"       % "8.0.0-play-28",
  "uk.gov.hmrc"       %% "agent-mtd-identifiers"      % "0.27.0-play-27",
  "uk.gov.hmrc"       %% "agent-kenshoo-monitoring"   % "4.8.0-play-28"
)

def testDeps(scope: String) = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % scope,
  "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0" % scope,
  "com.github.tomakehurst" % "wiremock-jre8" % "2.26.2" % scope,
  "uk.gov.hmrc" %% "reactivemongo-test" % "5.0.0-play-28" % scope,
  "org.jsoup" % "jsoup" % "1.14.2" % scope,
  "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % scope
)

lazy val root = (project in file("."))
  .settings(
    name := "agent-mapping-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.12",
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-missing-interpolator,_",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:implicitConversions",
      "-P:silencer:pathFilters=views;routes"),
    PlayKeys.playDefaultPort := 9438,
    resolvers := Seq(
      Resolver.typesafeRepo("releases")
    ),
    resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2",
    resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns),
    resolvers += "HMRC-local-artefacts-maven" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases-local",
    libraryDependencies ++= compileDeps ++ testDeps("test") ++ testDeps("it"),
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.0" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.0" % Provided cross CrossVersion.full
    ),
    publishingSettings,
    scoverageSettings,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    routesImport ++= Seq("uk.gov.hmrc.agentmappingfrontend.controllers.UrlBinders._"),
    scalafmtOnCompile in Compile := true,
    scalafmtOnCompile in Test := true
  )
  .configs(IntegrationTest)
  .settings(
    majorVersion := 0,
    Keys.fork in IntegrationTest := false,
    Defaults.itSettings,
    unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value,
    parallelExecution in IntegrationTest := false,
    scalafmtOnCompile in IntegrationTest := true
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)

inConfig(IntegrationTest)(scalafmtCoreSettings)
