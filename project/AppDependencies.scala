import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  private val bootstrapVer: String = "8.6.0"
  private val mongoVer: String = "1.9.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-30" % bootstrapVer,
    "com.typesafe.play"  %% "play-json-joda"             % "2.9.4", //TODO WG - remove joda
    "uk.gov.hmrc"        %% "play-frontend-hmrc-play-30" % "8.5.0",
    "uk.gov.hmrc"        %% "play-partials-play-30"      % "9.1.0",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-30"         % mongoVer,
    "uk.gov.hmrc"        %% "agent-mtd-identifiers"      % "1.15.0",
    "com.github.blemale" %% "scaffeine"                  % "5.2.1"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVer % Test,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % mongoVer     % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"      % "6.0.1"      % Test,
    "org.mockito"            %% "mockito-scala-scalatest" % "1.17.31"    % Test
  )
}
