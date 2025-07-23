#!/usr/bin/env bash
set -euo pipefail
IFS=$'\n\t'

MODULE_NAME="rabbit-consumer"
DOCKER_USERNAME="pepperjackdo"
IMAGE_NAME="${DOCKER_USERNAME}/${MODULE_NAME}"
TAG="latest"

if docker image inspect "${IMAGE_NAME}:${TAG}" > /dev/null 2>&1; then
  echo "Delete old ${IMAGE_NAME}:${TAG}"
  docker rmi -f "${IMAGE_NAME}:${TAG}"
else
  echo "No old image"
fi

echo "Maven clean package…"
./mvnw clean package -DskipTests

echo "Builder image ${IMAGE_NAME}:${TAG}…"
docker build \
  --file Docker \
  --tag "${IMAGE_NAME}:${TAG}" \
  .

echo "Finish"
