import sbt.Keys._
import scala.language.postfixOps

ThisBuild / version := sys.env.getOrElse("VERSION", "LOCAL")
ThisBuild / pushRemoteCacheTo := Some(MavenCache("local-cache", (ThisBuild / baseDirectory).value / "local-cache-tmp"))

val commonSettings = Seq(
  autoCompilerPlugins := true,
  sbtPlugin := true,
  organization := "nl.pragmasoft",
  scalaVersion := "2.12.19",
  scalacOptions := Seq(
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:higherKinds",
    "-language:existentials",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-encoding",
    "utf8",
    "-Xfatal-warnings"
  ),
  addCompilerPlugin("com.olegpy"   %% "better-monadic-for" % "0.3.1"),
  addCompilerPlugin("org.typelevel" % "kind-projector"     % "0.13.2" cross CrossVersion.full)
) ++ Publish.settings

lazy val root = project
  .in(file("."))
  .settings(commonSettings ++ Publish.settings)
  .settings(
    libraryDependencies ++= Seq(
      Defaults.sbtPluginExtra("com.jsuereth"      % "sbt-pgp"              % "2.1.1", "1.0", "2.12"),
      Defaults.sbtPluginExtra("org.xerial.sbt"    % "sbt-sonatype"         % "3.9.15", "1.0", "2.12"),
      Defaults.sbtPluginExtra("com.github.sbt" % "sbt-release" % "1.1.0", "1.0", "2.12"),
      Defaults.sbtPluginExtra("no.arktekk.sbt" % "aether-deploy" % "0.27.0", "1.0", "2.12"),
      Defaults.sbtPluginExtra("com.gilcloud"   % "sbt-gitlab"    % "0.1.2", "1.0", "2.12")
    )
  )
  .settings(name := "gitlab-package-registry")
