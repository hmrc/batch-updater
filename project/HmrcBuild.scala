import sbt.Keys._
import sbt._
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}
import uk.gov.hmrc.versioning.SbtGitVersioning


object HmrcBuild extends Build {
  import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

  val appName = "batch-updater"

  lazy val `batch-updater` = (project in file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
    .settings(majorVersion := 5)
    .settings(
      name := appName,
      libraryDependencies ++= appDependencies
    )

  val appDependencies = Seq(
    "uk.gov.hmrc"     %% "play-auditing"               % "4.3.0-play-25",
    "ch.qos.logback"  %  "logback-classic"             % "1.2.3",
    "org.scalatest"   %% "scalatest"                   % "2.2.6"   % "test",
    "org.pegdown"     %  "pegdown"                     % "1.6.0"   % "test",
    "org.scalamock"   %% "scalamock-scalatest-support" % "3.2.2"   % "test"
  )

}

