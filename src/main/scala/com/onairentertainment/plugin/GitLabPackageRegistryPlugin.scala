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

    def prepareSettings(): Seq[Setting[_]] = {
      val releasesRegistryUri   = EnvVariableHelper.getRequiredEnvironmentVariable(PackageReleasesRegistryUri)
      val releasesRegistryToken = EnvVariableHelper.getRequiredEnvironmentVariable(PackageReleasesRegistryToken)
      val releasesRegistryName  = "gitlab-releases"

      val prodRegistryUri   = EnvVariableHelper.getRequiredEnvironmentVariable(PackageRegistryUri)
      val prodRegistryToken = EnvVariableHelper.getRequiredEnvironmentVariable(PackageRegistryToken)
      val prodRegistryName  = "gitlab-prod"

      val repositories =
        Seq(
          MavenRepository(releasesRegistryName, releasesRegistryUri),
          MavenRepository(prodRegistryName, prodRegistryUri)
        )

      def authentication(token: EnvVariableHelper.EnvVariable): Authentication = {
        Authentication(
          user     = "user",
          password = "password",
          headers  = Seq((CustomAuthHeader, token)),
          optional = false,
          realmOpt = None
        )
      }

      val releasesAuthentication = authentication(releasesRegistryToken)
      val prodAuthentication     = authentication(prodRegistryToken)

      Seq(
        resolvers ++= repositories,
        csrConfiguration ~= (_.addRepositoryAuthentication(releasesRegistryName, releasesAuthentication)),
        csrConfiguration ~= (_.addRepositoryAuthentication(prodRegistryName, prodAuthentication)),
        updateClassifiers / csrConfiguration ~= (_.addRepositoryAuthentication(
          releasesRegistryName,
          releasesAuthentication
        )),
        updateClassifiers / csrConfiguration ~= (_.addRepositoryAuthentication(prodRegistryName, prodAuthentication)),
        publishMavenStyle := true,
        aether.AetherKeys.aetherCustomHttpHeaders := Map(
          CustomAuthHeader -> List(releasesRegistryToken, prodRegistryToken).mkString(",")
        )
      )
    }

    prepareSettings()
  }
}
