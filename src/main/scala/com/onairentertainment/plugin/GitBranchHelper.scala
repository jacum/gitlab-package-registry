package com.onairentertainment.plugin

import scala.sys.process.Process

object GitBranchHelper {

  private val CiCommitBranch  = "CI_COMMIT_BRANCH"
  private val CiCommitRefName = "CI_COMMIT_REF_NAME"

  private val ReleasePrefix = "release"

  def isReleaseBranch: Boolean = getBranchName.exists(_.startsWith(ReleasePrefix))

  private def getBranchName: Option[EnvVariableHelper.EnvVariable] =
    EnvVariableHelper
      .getEnvironmentVariable(CiCommitBranch)
      .orElse(EnvVariableHelper.getEnvironmentVariable(CiCommitRefName))
      .orElse(Process("git rev-parse --abbrev-ref HEAD").lineStream.headOption)
}
