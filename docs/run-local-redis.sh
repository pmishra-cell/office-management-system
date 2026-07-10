#!/usr/bin/env sh

set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ROOT_DIR="$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)"

cd "$ROOT_DIR"

echo "Starting Redis service via Docker Compose..."
docker compose -f docker-compose.redis.yml up -d

echo "Running app with local-redis profile..."
if mvn spring-boot:run -Dspring-boot.run.profiles=local-redis; then
	exit 0
else
	status=$?
fi

# Maven returns signal-style exit codes when the app is terminated (for example 143 on SIGTERM).
if [ "$status" -eq 130 ] || [ "$status" -eq 143 ]; then
	echo "Spring Boot run ended by termination signal (exit $status)."
	exit 0
fi

exit "$status"
