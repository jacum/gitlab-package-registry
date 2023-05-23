package com.onairentertainment.plugin

import com.gilcloud.sbt.gitlab.{GitlabCredentials, GitlabPlugin}
import com.onairentertainment.plugin.GitLabPackageRegistryPlugin.{
  CustomAuthHeader,
  PackageLiveRegistryToken,
  PackageRegistryToken
}
import sbt.Def
import sbt.Keys.*

import scala.sys.process.Process

object Publish {

  private def publishSettings(packageToken: String, packageId: Int): Seq[Def.Setting[_]] = {
    EnvVariableHelper.getEnvironmentVariable(packageToken) match {

      case Some(token) =>
        println(s"$packageToken found...")
        Seq(
          GitlabPlugin.autoImport.gitlabCredentials := Some(GitlabCredentials(CustomAuthHeader, token)),
          GitlabPlugin.autoImport.gitlabProjectId := Some(packageId),
          publishMavenStyle := true,
          aether.AetherKeys.aetherCustomHttpHeaders := Map(CustomAuthHeader -> token)
        )

      case _ =>
        println(s"$packageToken not found...")
        Seq.empty
    }
  }

  val Settings: Seq[Def.Setting[_]] = {
    val branchName = Process("git rev-parse --abbrev-ref HEAD").lineStream.headOption

    if (branchName.exists(_.startsWith("release")))
      publishSettings(PackageLiveRegistryToken, 390)
    else
      publishSettings(PackageRegistryToken, 71)
  }

  val DoNotPublishToRegistry: Seq[Def.Setting[_]] = Seq(
    publish / skip := true,
    publish := {},
    publishLocal := {}
  )

  val DoNotPublish: Seq[Def.Setting[_]] = DoNotPublishToRegistry ++ Seq(
    publishArtifact := false
  )
}
