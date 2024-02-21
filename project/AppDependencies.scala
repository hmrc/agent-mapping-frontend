import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val bootstrapVer: String = "7.23.0"
  private val mongoVer: String = "1.3.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-28" % bootstrapVer,
    "com.typesafe.play"  %% "play-json-joda"             % "2.9.4",
    "uk.gov.hmrc"        %% "play-frontend-hmrc-play-28" % "8.5.0",
    "uk.gov.hmrc"        %% "play-partials"              % "8.4.0-play-28",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-28"         % mongoVer,
    "uk.gov.hmrc"        %% "agent-mtd-identifiers"      % "1.14.0",
    "uk.gov.hmrc"        %% "agent-kenshoo-monitoring"   % "5.5.0",
    "com.github.blemale" %% "scaffeine"                  % "5.2.1"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % bootstrapVer % "test, it",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0"      % "test, it",
    "org.scalatestplus"      %% "mockito-3-12"            % "3.2.10.0"   % "test, it",
    "com.github.tomakehurst" %  "wiremock-jre8"           % "2.26.2"     % "test, it",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28" % mongoVer     % "test, it",
    "org.jsoup"              %  "jsoup"                   % "1.15.4"     % "test, it",
    "com.vladsch.flexmark"   %  "flexmark-all"            % "0.36.8"    % "test, it"
  )
}
