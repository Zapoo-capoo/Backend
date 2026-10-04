#!/usr/bin/env bash
# Runs ON the server (/opt/zapoo). Starts the stack with the given image tag and rolls back to the previous tag if
# the new one does not become healthy.
#
#   ./deploy.sh sha-1a2b3c4
#
# Needs next to it: docker-compose.prod.yml, .env, and the ./config folder (production config files).
set -euo pipefail

cd "$(dirname "$0")"

TAG="${1:?usage: ./deploy.sh <image-tag>}"
STATE_FILE=".deployed_tag"
WAIT_SECONDS="${WAIT_SECONDS:-900}"
JAVA_SERVICES=(discovery-service config-service identity-service profile-service chat-service storage-service api-gateway)

compose() {
  docker compose --env-file .env -f docker-compose.prod.yml "$@"
}

for required in .env docker-compose.prod.yml config; do
  [ -e "$required" ] || { echo "Missing $required in $(pwd)" >&2; exit 2; }
done

PREVIOUS_TAG="$(cat "$STATE_FILE" 2>/dev/null || true)"
echo "==> Deploying image tag: $TAG (previous: ${PREVIOUS_TAG:-none})"

export IMAGE_TAG="$TAG"

echo "==> Pulling images"
compose pull "${JAVA_SERVICES[@]}"

echo "==> Starting the stack (waits until every service is healthy, at most ${WAIT_SECONDS}s)"
if compose up -d --remove-orphans --wait --wait-timeout "$WAIT_SECONDS"; then
  echo "$TAG" > "$STATE_FILE"
  echo "==> Deployed $TAG"
  compose ps
  # Keep disk usage in check: drop dangling layers and images nobody has used for a week
  docker image prune -f > /dev/null
  docker image prune -af --filter "until=168h" > /dev/null || true
  exit 0
fi

echo "!! $TAG did not become healthy" >&2
compose ps >&2 || true
for service in "${JAVA_SERVICES[@]}"; do
  echo "----- last logs of $service" >&2
  compose logs --no-color --tail 40 "$service" >&2 || true
done

if [ -n "$PREVIOUS_TAG" ] && [ "$PREVIOUS_TAG" != "$TAG" ]; then
  echo "==> Rolling back to $PREVIOUS_TAG" >&2
  export IMAGE_TAG="$PREVIOUS_TAG"
  if compose up -d --remove-orphans --wait --wait-timeout "$WAIT_SECONDS"; then
    echo "==> Rolled back to $PREVIOUS_TAG, the failed deployment of $TAG is NOT live" >&2
  else
    echo "!! The rollback to $PREVIOUS_TAG is not healthy either, check the server" >&2
  fi
else
  echo "!! No previous deployment to roll back to" >&2
fi
exit 1
