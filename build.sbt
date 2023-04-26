import sbt.Keys.resolvers

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := """uk\.gov\.hmrc\.BuildInfo;.*\.Routes;.*\.RoutesPrefix;.*Filters?;MicroserviceAuditConnector;Module;GraphiteStartUp;.*\.Reverse[^.]*""",
    ScoverageKeys.coverageMinimumStmtTotal := 80.00,
    //ScoverageKeys.coverageMinimumStmtPerFile := 80.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )
}

val silencerVersion = "1.7.8"

lazy val root = (project in file("."))
  .settings(
    name := "agent-mapping-frontend",
    organization := "uk.gov.hmrc",
    scalaVersion := "2.12.15",
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
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases"),
      "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2",
      Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns),
    ),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    ),
    scoverageSettings,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    routesImport ++= Seq("uk.gov.hmrc.agentmappingfrontend.controllers.UrlBinders._"),
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile := true
  )
  .configs(IntegrationTest)
  .settings(
    majorVersion := 0,
    IntegrationTest / Keys.fork := false,
    Defaults.itSettings,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / scalafmtOnCompile := true
  )
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  //v Required to prevent https://github.com/scalatest/scalatest/issues/1427 (unstable build due to failure to read test reports)
  .disablePlugins(JUnitXmlReportPlugin)

inConfig(IntegrationTest)(scalafmtCoreSettings)
