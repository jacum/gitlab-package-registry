package com.onairentertainment.plugin

import lmcoursier.definitions.Authentication
import lmcoursier.syntax.*
import sbt.Keys.{csrConfiguration, publishMavenStyle, resolvers, updateClassifiers}
import sbt.librarymanagement.MavenRepository
import sbt.{AutoPlugin, PluginTrigger, Setting}

object GitLabPackageRegistryPlugin extends AutoPlugin {

  val PackageRegistryUri       = "PACKAGES_RW_URI"
  val PackageRegistryToken     = "PACKAGES_RW_TOKEN"
  val PackageRegistryName      = "gitlab-prod"
  val PackageRegistryProjectId = 71

  val PackageReleasesRegistryUri       = "PACKAGES_LIVE_RW_URI"
  val PackageReleasesRegistryToken     = "PACKAGES_LIVE_RW_TOKEN"
  val PackageReleasesRegistryName      = "gitlab-releases"
  val PackageReleasesRegistryProjectId = 390

  val CustomAuthHeader = "Private-Token"

  override def trigger: PluginTrigger = allRequirements

  override def projectSettings: Seq[Setting[_]] = {

    def authentication(token: EnvVariableHelper.EnvVariable): Authentication = {
      Authentication(
        user     = "user",
        password = "password",
        headers  = Seq((CustomAuthHeader, token)),
        optional = false,
        realmOpt = None
      )
    }

    def prepareSettings(tokenName: String, uriName: String, registryName: String): Seq[Setting[_]] = {
      val registryUri   = EnvVariableHelper.getRequiredEnvironmentVariable(uriName)
      val registryToken = EnvVariableHelper.getRequiredEnvironmentVariable(tokenName)

      val registryAuthentication = authentication(registryToken)

      Seq(
        resolvers += MavenRepository(registryName, registryUri),
        csrConfiguration ~= (_.addRepositoryAuthentication(registryName, registryAuthentication)),
        updateClassifiers / csrConfiguration ~= (_.addRepositoryAuthentication(registryName, registryAuthentication)),
        publishMavenStyle := true,
        aether.AetherKeys.aetherCustomHttpHeaders := Map(CustomAuthHeader -> registryToken)
      )
    }

    val releaseSettings =
      if (GitBranchHelper.isReleaseBranch || GitBranchHelper.isUpdateReleaseBranch)
        prepareSettings(PackageReleasesRegistryToken, PackageReleasesRegistryUri, PackageReleasesRegistryName)
      else Seq.empty[Setting[_]]

    prepareSettings(PackageRegistryToken, PackageRegistryUri, PackageRegistryName) ++ releaseSettings
  }
}
