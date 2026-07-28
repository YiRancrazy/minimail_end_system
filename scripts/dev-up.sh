#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps