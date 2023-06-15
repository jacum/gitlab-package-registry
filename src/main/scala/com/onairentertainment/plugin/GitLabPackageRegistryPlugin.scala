package com.onairentertainment.plugin

import lmcoursier.definitions.Authentication
import lmcoursier.syntax.*
import sbt.Keys.{csrConfiguration, publishMavenStyle, resolvers, updateClassifiers}
import sbt.librarymanagement.MavenRepository
import sbt.{AutoPlugin, PluginTrigger, Setting}

object GitLabPackageRegistryPlugin extends AutoPlugin {

  val PackageRegistryUri   = "PACKAGES_RW_URI"
  val PackageRegistryToken = "PACKAGES_RW_TOKEN"

  val PackageReleasesRegistryUri   = "PACKAGES_LIVE_RW_URI"
  val PackageReleasesRegistryToken = "PACKAGES_LIVE_RW_TOKEN"

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
      val prodRegistryUri   = EnvVariableHelper.getRequiredEnvironmentVariable(uriName)
      val prodRegistryToken = EnvVariableHelper.getRequiredEnvironmentVariable(tokenName)

      val prodAuthentication = authentication(prodRegistryToken)

      Seq(
        resolvers += MavenRepository(registryName, prodRegistryUri),
        csrConfiguration ~= (_.addRepositoryAuthentication(registryName, prodAuthentication)),
        updateClassifiers / csrConfiguration ~= (_.addRepositoryAuthentication(registryName, prodAuthentication)),
        publishMavenStyle := true,
        aether.AetherKeys.aetherCustomHttpHeaders := Map(CustomAuthHeader -> prodRegistryToken)
      )
    }

    val branchName = EnvVariableHelper
      .getEnvironmentVariable("CI_COMMIT_BRANCH")
      .orElse(EnvVariableHelper.getEnvironmentVariable("CI_COMMIT_REF_NAME"))

    val releaseSettings = {
      if (EnvVariableHelper.getEnvironmentVariable(PackageReleasesRegistryToken).nonEmpty)
        prepareSettings(PackageReleasesRegistryToken, PackageReleasesRegistryUri, "gitlab-releases")
      else if (branchName.exists(_.startsWith("release"))) {
        println(s"$PackageReleasesRegistryToken could not be found. Make your releases/* branch protected")
        Seq.empty[Setting[_]]
      } else Seq.empty[Setting[_]]
    }

    prepareSettings(PackageRegistryToken, PackageRegistryUri, "gitlab-prod") ++ releaseSettings
  }
}
