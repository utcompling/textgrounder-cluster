import com.github.retronym.SbtOneJar
import sbt._
import Keys._

object TextGrounderClusterBuild extends Build {
  def standardSettings = Seq(
    exportJars := true
  ) ++ Defaults.defaultSettings

  lazy val main = Project("textgrounder-cluster", file(".")) dependsOn(scoobi)

  lazy val scoobi = Project("scoobi", file("scoobi"))

}

