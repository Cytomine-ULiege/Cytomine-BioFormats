#!/bin/bash

set -e

function get_git_head_branch {
  # Go to root
  cd $(git rev-parse --show-toplevel)

  echo $(git rev-parse --abbrev-ref HEAD)
}

function get_git_tag {
  # Go to root
  cd $(git rev-parse --show-toplevel)

  lastGitTag=$(git describe --long --dirty)
  if [[ $lastGitTag =~ v[0-9]+.[0-9]+.[0-9]+-0-[0-9a-g]{8,9}$ ]]; then
    echo $lastGitTag
  else
    echo ${1:-get_git_head_branch}
  fi
}

function get_version_number {
  # Go to root
  cd $(git rev-parse --show-toplevel)

  lastGitTag=$(git describe --long --dirty)
  if [[ $lastGitTag =~ v[0-9]+.[0-9]+.[0-9]+-0-[0-9a-g]{8,9}$ ]]; then
    # official release x.y.z
    versionNumber=$(echo $lastGitTag | sed -r "s/v([0-9]+\.[0-9]+\.[0-9]+)-[0-9]+-.+/\1/")
  else
    # rc: branchname + date + 'SNAPSHOT'
    versionNumber=${1:-get_git_head_branch}-$(date "+%Y%m%d%H%M%S")-SNAPSHOT
  fi
  echo $versionNumber
}

