import sbt._
import Keys._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys._

object Publish {

  val SuppressJavaDocsAndSources = Seq(
    doc / sources := Seq(),
    packageDoc / publishArtifact := false,
    packageSrc / publishArtifact := false
  )

  private val pomSettings = Seq(
    licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT")),
    homepage := Some(url("https://github.com/jacum/gitlab-package-registry")),
    scmInfo := Some(
      ScmInfo(
        browseUrl  = url("https://github.com/jacum/gitlab-package-registry"),
        connection = "scm:git@github.com:jacum/gitlab-package-registry.git"
      )
    ),
    pomExtra := (
      <developers>
        <developer>
          <id>PragmaSoft</id>
          <name>PragmaSoft</name>
        </developer>
      </developers>
    ),
    publishMavenStyle := true,
    sbtPluginPublishLegacyMavenStyle := false,
    Test / publishArtifact := false,
    packageDoc / publishArtifact := true,
    packageSrc / publishArtifact := true,
    pomIncludeRepository := (_ => false)
  )

  val ReleaseToSonatype = pomSettings ++ Seq(
    credentials ++= Seq(
      Credentials(
        "Sonatype Central",
        "central.sonatype.com",
        sys.env.getOrElse("SONATYPE_USER", ""),
        sys.env.getOrElse("SONATYPE_PASSWORD", "")
      ),
      Credentials(
        "GnuPG Key ID",
        "gpg",
        "80639E9F764EA1049652FDBBDA743228BD43ED35", // key identifier
        "ignored"                                   // sbt-pgp uses PGP_PASSPHRASE; this field is ignored
      )
    ),
    sonatypeProfileName := "nl.pragmasoft",
    // Central Publishing Portal (OSSRH EOL)
    sonatypeCredentialHost := "central.sonatype.com",
    sonatypeRepository := "https://central.sonatype.com/api",
    publishTo := sonatypePublishToBundle.value,
    releaseIgnoreUntrackedFiles := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      setReleaseVersion,
      releaseStepCommand("sonatypeBundleClean"),
      releaseStepCommand("publishSigned"),
      releaseStepCommand("sonatypeCentralUpload"),
      releaseStepCommand("sonatypeCentralRelease")
    )
  )

  private val nexusRegistry = sys.env.get("NEXUS_REGISTRY")

  val PublishToNexus: Seq[Def.Setting[_]] = pomSettings ++ Seq(
    credentials ++= {
      for {
        registry <- nexusRegistry
        username <- sys.env.get("NEXUS_USERNAME")
        password <- sys.env.get("NEXUS_PASSWORD")
      } yield Credentials("Sonatype Nexus Repository Manager", registry, username, password)
    }.toSeq,
    publishTo := nexusRegistry.map { registry =>
      if (isSnapshot.value)
        "snapshots" at s"https://$registry/repository/maven-snapshots/"
      else
        "releases" at s"https://$registry/repository/maven-releases/"
    }
  )

  val settings =
    if (sys.env.contains("SONATYPE_USER")) {
      println(s"Releasing to Sonatype Central as ${sys.env("SONATYPE_USER")}")
      ReleaseToSonatype
    } else if (nexusRegistry.isDefined) {
      println(s"Publishing to Nexus at ${nexusRegistry.get}")
      PublishToNexus
    } else SuppressJavaDocsAndSources

}
