#!/usr/bin/env bash
# Runs ON the server (/opt/zapoo). Starts the stack with the given image tag and rolls back to the previous tag if
# the new one does not become healthy.
#
#   ./deploy.sh sha-1a2b3c4
#
# Needs next to it: docker-compose.prod.yml, .env, and the ./config folder (production config files).
#
# The GitHub workflow starts this script detached (nohup) and follows .deploy_status / deploy.log, so a dropped SSH
# connection cannot kill a deployment halfway. .deploy_status holds one word: running, success or failed.
set -euo pipefail

cd "$(dirname "$0")"

TAG="${1:?usage: ./deploy.sh <image-tag>}"
STATE_FILE=".deployed_tag"
STATUS_FILE=".deploy_status"
STAGE_WAIT_SECONDS="${STAGE_WAIT_SECONDS:-420}"
JAVA_SERVICES=(discovery-service config-service identity-service profile-service chat-service storage-service notification-service api-gateway)

compose() {
  docker compose --env-file .env -f docker-compose.prod.yml "$@"
}

# Starting everything at once makes a small machine (2 vCPU) crawl: MySQL initialises its data, Kafka, Neo4j and several
# JVMs all want the CPU and the disk at the same moment. So the stack is started in groups and every group has to be
# healthy before the next one begins. Order: databases, then Kafka/Neo4j, then config > discovery > profile > identity >
# chat/storage > gateway.
STAGES=(
  "postgres mysql mongo redis"
  "kafka neo4j"
  "config-service discovery-service"
  "profile-service"
  "identity-service"
  "chat-service storage-service"
  "notification-service"
  "api-gateway"
)

# Brings the stack up with the image tag in $IMAGE_TAG, stage by stage. Returns non-zero at the first stage that fails
start_stack() {
  local stage
  for stage in "${STAGES[@]}"; do
    echo "==> $(date -u +%H:%M:%S) Starting: $stage"
    # shellcheck disable=SC2086 # the stage is a list of service names
    compose up -d --remove-orphans --wait --wait-timeout "$STAGE_WAIT_SECONDS" $stage || {
      echo "!! $(date -u +%H:%M:%S) Stage failed: $stage" >&2
      return 1
    }
  done
}

echo running > "$STATUS_FILE"
# Anything that ends the script without reaching a clean "success" is a failure for whoever is watching
trap 'rc=$?; if [ "$(cat "$STATUS_FILE" 2>/dev/null)" = "running" ]; then echo failed > "$STATUS_FILE"; fi; exit $rc' EXIT

for required in .env docker-compose.prod.yml config; do
  [ -e "$required" ] || { echo "Missing $required in $(pwd)" >&2; exit 2; }
done

PREVIOUS_TAG="$(cat "$STATE_FILE" 2>/dev/null || true)"
echo "==> $(date -u +%H:%M:%S) Deploying image tag: $TAG (previous: ${PREVIOUS_TAG:-none})"

export IMAGE_TAG="$TAG"

echo "==> $(date -u +%H:%M:%S) Pulling images"
compose pull --quiet "${JAVA_SERVICES[@]}"
echo "==> $(date -u +%H:%M:%S) Images pulled"

echo "==> $(date -u +%H:%M:%S) Starting the stack in stages (each stage may take up to ${STAGE_WAIT_SECONDS}s to become healthy)"
if start_stack; then
  echo "$TAG" > "$STATE_FILE"
  echo success > "$STATUS_FILE"
  echo "==> $(date -u +%H:%M:%S) Deployed $TAG"
  compose ps
  # Keep disk usage in check: drop dangling layers and images nobody has used for a week
  docker image prune -f > /dev/null
  docker image prune -af --filter "until=168h" > /dev/null || true
  exit 0
fi

echo "!! $TAG did not become healthy" >&2
compose ps >&2 || true
echo "----- machine load: $(uptime)" >&2
free -h >&2 || true
for service in "${JAVA_SERVICES[@]}"; do
  echo "----- last logs of $service" >&2
  compose logs --no-color --tail 40 "$service" >&2 || true
done

if [ -n "$PREVIOUS_TAG" ] && [ "$PREVIOUS_TAG" != "$TAG" ]; then
  echo "==> Rolling back to $PREVIOUS_TAG" >&2
  export IMAGE_TAG="$PREVIOUS_TAG"
  if start_stack; then
    echo "==> Rolled back to $PREVIOUS_TAG, the failed deployment of $TAG is NOT live" >&2
  else
    echo "!! The rollback to $PREVIOUS_TAG is not healthy either, check the server" >&2
  fi
else
  echo "!! No previous deployment to roll back to" >&2
fi
echo failed > "$STATUS_FILE"
exit 1
