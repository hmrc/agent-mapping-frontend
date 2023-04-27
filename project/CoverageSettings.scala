import sbt.Keys.parallelExecution
import sbt.{Setting, Test}
import scoverage.ScoverageKeys

object CoverageSettings {
  private val excludedPackages = List(
    "<empty>",
    ".*Routes.*",
  ).mkString(";")


  private val excludedFiles = Seq(
    ".*template",
    ".*MappingArnRepository",
    ".*UriPathEncoding",
    ".*SimpleObjectBinder",
    ".*UrlBinders",
    ".*TaskListMappingRepository",
  ).mkString(";")

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages,
    ScoverageKeys.coverageExcludedFiles := excludedFiles,
    ScoverageKeys.coverageMinimumStmtTotal := 95.00,
    ScoverageKeys.coverageMinimumStmtPerFile := 80.00,
    ScoverageKeys.coverageMinimumBranchTotal := 89.00,
    ScoverageKeys.coverageMinimumBranchPerFile := 70.00,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false
  )

}
