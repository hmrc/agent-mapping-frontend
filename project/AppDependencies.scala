import play.sbt.PlayImport.ws
import sbt.*

object AppDependencies {

  private val playVer: String = "play-30"
  private val bootstrapVer: String = "10.7.0"
  private val mongoVer: String = "2.13.0"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVer" % bootstrapVer,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-$playVer"         % mongoVer,
    "uk.gov.hmrc"       %% s"play-frontend-hmrc-$playVer" % "13.11.0",
    "uk.gov.hmrc"       %% s"domain-$playVer"             % "13.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVer"  % bootstrapVer,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test-$playVer" % mongoVer
  ).map(_ % Test)
}
