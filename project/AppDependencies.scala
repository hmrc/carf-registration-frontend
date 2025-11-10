import sbt.*

object AppDependencies {

  private val bootstrapVersion        = "9.19.0"
  private val hmrcMongoVersion        = "2.10.0"
  private val playFrontendHmrcVersion = "12.20.0"
  private val catsCoreVersion         = "2.13.0"
  private val commonsValidatorVersion = "1.10.0"

  val compile = Seq(
    "commons-validator"  % "commons-validator"          % commonsValidatorVersion,
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % playFrontendHmrcVersion,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "org.typelevel"     %% "cats-core"                  % catsCoreVersion
  )

  val test = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"    %% "scalacheck-1-17"         % "3.2.17.0",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"   % "1.1.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
