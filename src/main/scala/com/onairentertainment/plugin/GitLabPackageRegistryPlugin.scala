package com.onairentertainment.plugin

import lmcoursier.definitions.Authentication
import lmcoursier.syntax.*
import sbt.Keys.{csrConfiguration, publishMavenStyle, resolvers, updateClassifiers}
import sbt.librarymanagement.MavenRepository
import sbt.{AutoPlugin, PluginTrigger, Setting}

import scala.sys.process.Process

object GitLabPackageRegistryPlugin extends AutoPlugin {

  val PackageRegistryUri   = "PACKAGES_RW_URI"
  val PackageRegistryToken = "PACKAGES_RW_TOKEN"

  val PackageLiveRegistryUri   = "PACKAGES_LIVE_RW_URI"
  val PackageLiveRegistryToken = "PACKAGES_LIVE_RW_TOKEN"

  val CustomAuthHeader = "Private-Token"

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[_]] = {

    def prepareSettings(registry: String, registryToken: String): Seq[Setting[_]] = {
      val uri        = EnvVariableHelper.getRequiredEnvironmentVariable(registry)
      val token      = EnvVariableHelper.getRequiredEnvironmentVariable(registryToken)
      val repository = MavenRepository("gitlab", uri)
      val authentication = Authentication(
        user     = "user",
        password = "password",
        headers  = Seq((CustomAuthHeader, token)),
        optional = false,
        realmOpt = None
      )

      Seq(
        resolvers += repository,
        csrConfiguration ~= (_.addRepositoryAuthentication(repository.name, authentication)),
        updateClassifiers / csrConfiguration := csrConfiguration.value,
        publishMavenStyle := true,
        aether.AetherKeys.aetherCustomHttpHeaders := Map(CustomAuthHeader -> token)
      )
    }

    val branchName = Process("git rev-parse --abbrev-ref HEAD").lineStream.headOption

    if (branchName.exists(_.startsWith("release")))
      prepareSettings(PackageLiveRegistryUri, PackageLiveRegistryToken)
    else
      prepareSettings(PackageRegistryUri, PackageRegistryToken)
  }
}
