import sbt.*

object AppDependencies {

  private val bootstrapVersion        = "10.8.0"
  private val hmrcMongoVersion        = "2.13.0"
  private val playFrontendHmrcVersion = "13.13.0"
  private val catsCoreVersion         = "2.13.0"
  private val commonsValidatorVersion = "1.10.0"
  private val libphonenumberVersion   = "8.13.47"
  private val cryptoVersion           = "8.4.0"

  val compile: Seq[ModuleID] = Seq(
    "commons-validator"             % "commons-validator"          % commonsValidatorVersion,
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                  %% "play-frontend-hmrc-play-30" % playFrontendHmrcVersion,
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo"            %% "hmrc-mongo-play-30"         % hmrcMongoVersion,
    "com.googlecode.libphonenumber" % "libphonenumber"             % libphonenumberVersion,
    "org.typelevel"                %% "cats-core"                  % catsCoreVersion,
    "uk.gov.hmrc"                  %% "crypto-json-play-30"        % cryptoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"          %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"    %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"    %% "scalacheck-1-17"         % "3.2.17.0",
    "io.github.wolfendale" %% "scalacheck-gen-regexp"   % "1.1.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
