#!/bin/bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT="$( cd $DIR && cd .. && pwd )"
ME=$(basename $0)

source $DIR/build-version.sh

NAMESPACE=${2:-cytomine}
DOCKER_REGISTRY=${3:-docker.io}
IMAGE_NAME=${4:-bioformat}
BRANCH=${5:-$(get_git_head_branch)}
TAG=$(get_git_tag $BRANCH)
VERSION_NUMBER=${VERSION_NUMBER:-$(get_version_number $BRANCH)}

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

  mkdir -p ./ci
  containerId=$(docker create $image)
  docker start -ai  $containerId
  docker cp $containerId:/app/build/libs/cytomine-bioformats-wrapper.jar ./ci
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

# Commands
case $1 in
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