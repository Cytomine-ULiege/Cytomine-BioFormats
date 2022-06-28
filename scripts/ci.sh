#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$( cd $DIR && cd .. && pwd )"
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

  mkdir -p ./output
  containerId=$(docker create $image)
  docker start -ai  $containerId
  docker cp $containerId:/app/build/libs/cytomine-bioformats-wrapper.jar ./output
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