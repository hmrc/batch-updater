import java.net.URL

import sbt.Keys._
import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning


object HmrcBuild extends Build {

  import BuildDependencies._
  import uk.gov.hmrc.DefaultBuildSettings._

  val appName = "batch-updater"

  lazy val `batch-updater` = (project in file("."))
    .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning)
    .settings(
      name := appName,
      targetJvm := "jvm-1.7",
      libraryDependencies ++= Seq(
        Compile.httpVerbs,
        Test.scalaTest,
        Test.pegdown,
        Test.scalaMock
      ),
      Developers()
    )
}

private object BuildDependencies {

  object Compile {
    val httpVerbs = "uk.gov.hmrc" %% "http-verbs" % "1.4.0"
    val playIteratees = "com.typesafe.play" %% "play-iteratees" % "2.3.8"
    val play = "com.typesafe.play" %% "play" % "2.3.8"
  }

  sealed abstract class Test(scope: String) {
    val scalaTest = "org.scalatest" %% "scalatest" % "2.2.4" % scope
    val pegdown = "org.pegdown" % "pegdown" % "1.5.0" % scope
    val scalaMock = "org.scalamock" %% "scalamock-scalatest-support" % "3.2" % scope
  }

  object Test extends Test("test")

}

object Developers {

  def apply() = developers := List(
    Developer("howyp", "Howard Perrin", "howard.perrin@digital.hmrc.gov.uk", new URL("http://www.zuhlke.co.uk/"))
  )
}
