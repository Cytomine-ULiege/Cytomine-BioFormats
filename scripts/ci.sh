#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$( cd $DIR && cd .. && pwd )"
OUTPUT=$ROOT/output
ME=$(basename $0)

function getGitHeadBranch {
  # Go to root
  cd $(git rev-parse --show-toplevel)

  echo $(git rev-parse --abbrev-ref HEAD)
}

function getGitTag {
  # Go to root
  cd $(git rev-parse --show-toplevel)

  lastGitTag=$(git describe --long --dirty)
  if [[ $lastGitTag =~ v[0-9]+.[0-9]+.[0-9]+-0-[0-9a-g]{8,9}$ ]]; then
    echo $lastGitTag
  else
    echo ${BRANCH:-$(getGitHeadBranch)}
  fi
}

function isOfficialRelease {
  # Go to root
  cd $(git rev-parse --show-toplevel)

  lastGitTag=$(git describe --long --dirty --tags)
  if [[ $lastGitTag =~ v[0-9]+.[0-9]+.[0-9]+-0-[0-9a-g]{8,9}$ ]]; then
    echo "true"
  else
    echo "false"
  fi
}

function getVersionNumber {
  # Go to root
  cd $(git rev-parse --show-toplevel)

  lastGitTag=$(git describe --long --dirty --tags)
  if [[ $lastGitTag =~ v[0-9]+.[0-9]+.[0-9]+-0-[0-9a-g]{8,9}$ ]]; then
    # official release x.y.z
    versionNumber=$(echo $lastGitTag | sed -r "s/v([0-9]+\.[0-9]+\.[0-9]+)-[0-9]+-.+/\1/")
  else
    # rc: branchname + date + 'SNAPSHOT'
    versionNumber=${BRANCH:-$(getGitHeadBranch)}-$(date "+%Y%m%d%H%M%S")-SNAPSHOT
  fi
  echo $versionNumber
}

function printInfo() {
  echo "************************************** $ME - $1 ******************************************"
  echo "$ME - $1: Git branch/tag is $TAG"
  echo "$ME - $1: Version number is $VERSION_NUMBER"
  echo "$ME - $1: Docker Registry is $DOCKER_REGISTRY"
  echo "$ME - $1: Docker Namespace is $NAMESPACE"
}

function downloadDependencies() {
  printInfo "downloadDependencies"

  image=$NAMESPACE/$IMAGE_NAME-download-dependencies:v$VERSION_NUMBER
  docker build \
    -f $DIR/docker/Dockerfile \
    --target dependencies \
    -t $image $ROOT

  containerDownloadDependenciesId=$(docker create $image)
  docker rm $containerDownloadDependenciesId
}

function buildJar() {
  printInfo "buildJar"

  image=$NAMESPACE/$IMAGE_NAME-jar:v$VERSION_NUMBER
  docker build \
    -f $DIR/docker/Dockerfile \
    --target builder \
    --build-arg VERSION_NUMBER=$VERSION_NUMBER \
    -t $image $ROOT

  mkdir -p $OUTPUT
  containerId=$(docker create $image)
  docker start -ai  $containerId
  docker cp $containerId:/app/build/libs/cytomine-bioformats-wrapper.jar $OUTPUT
  docker rm $containerId
}

function publishJar() {
  printInfo "publishJar"
  # TODO
}

function buildDockerImage() {
  printInfo "buildDockerImage"

  image=$NAMESPACE/$IMAGE_NAME:v$VERSION_NUMBER
  docker build \
    -f $DIR/docker/Dockerfile \
    --build-arg VERSION_NUMBER=$VERSION_NUMBER \
    -t $image $ROOT
}

function publishDockerImage() {
  printInfo "publishDockerImage"

  image=$NAMESPACE/$IMAGE_NAME:v$VERSION_NUMBER
  docker push $DOCKER_REGISTRY/$image
}

function imageDockerExists() {
  docker inspect $1 > /dev/null; echo $?
}

function cleanDocker() {
  printInfo "cleanDocker"
  image=$NAMESPACE/$IMAGE_NAME-download-dependencies:v$VERSION_NUMBER
  if [[ $(imageDockerExists $image) ]]; then
    docker rmi $image
  fi

  image=$NAMESPACE/$IMAGE_NAME-jar:v$VERSION_NUMBER
  if [[ $(imageDockerExists $image) ]]; then
    docker rmi $image
  fi
}

function publishGithub() {
  if [[ $(isOfficialRelease) == "true" ]]; then
    GITHUB_REPO=${GITHUB_REPO:?"GITHUB_REPO is unset. Abort."}
    GITHUB_RELEASE_USER=${GITHUB_RELEASE_USER:?"GITHUB_RELEASE_USER is unset. Abort."}
    GITHUB_RELEASE_TOKEN=${GITHUB_RELEASE_TOKEN:?"GITHUB_RELEASE_TOKEN is unset. Abort."}

    # Create a release
    release=$(
      curl https://api.github.com/repos/${GITHUB_REPO}/releases \
        -X POST \
        --trace-ascii - \
        -H "Accept: application/vnd.github.v3+json" \
        -H "Authorization: token ${GITHUB_RELEASE_TOKEN}" \
        --data @<(cat <<EOF
        {
          "tag_name":"v$VERSION_NUMBER",
          "generate_release_notes":true
        }
EOF
        )
    )

    upload_url=$(echo "$release" |  sed -n -e 's/"upload_url":\ "\(.*\+\){?name,label}",/\1/p' | sed 's/[[:blank:]]//g')
    # Upload the artifact
    curl $upload_url?name=cytomine-bioformats-wrapper.jar \
      -X POST \
      -H "Accept: application/vnd.github.v3+json" \
      -H "Authorization: token ${GITHUB_RELEASE_TOKEN}" \
      -H "Content-Type: application/java-archive" \
      --data-binary @$OUTPUT/cytomine-bioformats-wrapper.jar
  else
    echo "Not an official release, do not publish it on Github."
  fi
}

#### Variables
NAMESPACE=${2:-cytomine}
DOCKER_REGISTRY=${3:-docker.io}
IMAGE_NAME=${4:-bioformat}
BRANCH=${5:-$(getGitHeadBranch)}
TAG=$(getGitTag $BRANCH)
VERSION_NUMBER=${VERSION_NUMBER:-$(getVersionNumber $BRANCH)}

#### Commands
case $1 in
    "version-number")
        echo $VERSION_NUMBER
        ;;
    "is-official-release")
        isOfficialRelease
        ;;
    "dependencies")
        downloadDependencies
        ;;
    "build-jar")
        buildJar
        ;;
    "publish-jar")
        publishJar
        ;;
    "build-docker")
        buildDockerImage
        ;;
    "publish-docker")
        publishDockerImage
        ;;
    "clean-docker")
        cleanDocker
        ;;
    "publish-github")
        publishGithub
       ;;
    "build")
        downloadDependencies
        buildJar
        buildDockerImage
        ;;
    "all")
        downloadDependencies
        buildJar
        publishJar
        buildDockerImage
        publishDockerImage
        cleanDocker
        ;;
    *)
        echo "No command found."
esac