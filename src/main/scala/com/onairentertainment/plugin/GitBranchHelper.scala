package com.onairentertainment.plugin

import scala.sys.process.Process

object GitBranchHelper {

  private val CiCommitBranch  = "CI_COMMIT_BRANCH"
  private val CiCommitRefName = "CI_COMMIT_REF_NAME"

  private val ReleasePrefix       = "release"
  private val UpdateReleasePrefix = "update/release"

  def isReleaseBranch: Boolean = getBranchName.exists(_.startsWith(ReleasePrefix))

  def isUpdateReleaseBranch: Boolean = getBranchName.exists(_.startsWith(UpdateReleasePrefix))

  private def getBranchName: Option[EnvVariableHelper.EnvVariable] =
    EnvVariableHelper
      .getEnvironmentVariable(CiCommitBranch)
      .orElse(EnvVariableHelper.getEnvironmentVariable(CiCommitRefName))
      .orElse(Process("git rev-parse --abbrev-ref HEAD").lineStream.headOption)
}
