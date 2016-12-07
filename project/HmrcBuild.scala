import java.net.URL

import sbt.Keys._
import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning


object HmrcBuild extends Build {

  val appName = "batch-updater"

  lazy val `batch-updater` = (project in file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      name := appName,
      libraryDependencies ++= appDependencies
    )

  val appDependencies = Seq(
    "uk.gov.hmrc"     %% "play-auditing"               % "2.4.0",
    "org.scalatest"   %% "scalatest"                   % "2.2.4"   % "test",
    "org.pegdown"     %  "pegdown"                     % "1.5.0"   % "test",
    "org.scalamock"   %% "scalamock-scalatest-support" % "3.2"     % "test"
  )

}

