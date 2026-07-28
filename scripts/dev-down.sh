#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
docker compose -f infra/docker-compose.yml down