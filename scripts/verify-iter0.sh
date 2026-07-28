#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

say() { printf '\n=== %s ===\n' "$1"; }
fail() { printf '\nFAIL: %s\n' "$1"; exit 1; }

say "1/5 mvn verify (skip network-bound surefire)"
./mvnw -q verify || fail "mvn verify"

say "2/5 docker compose up"
./scripts/dev-up.sh || fail "docker compose"

say "3/5 wait for nacos on :8848"
for i in $(seq 1 30); do
  if curl -fs http://127.0.0.1:8848/nacos/ >/dev/null 2>&1; then break; fi
  sleep 2
done
curl -fs http://127.0.0.1:8848/nacos/ >/dev/null || fail "nacos not up after 60s"

say "4/5 start id-service + auth-service in background"
( SPRING_PROFILES_ACTIVE=test ./mvnw -q -pl minimall-id-service spring-boot:run ) >/tmp/id.log 2>&1 &
ID_PID=$!
( SPRING_PROFILES_ACTIVE=test ./mvnw -q -pl minimall-auth-service spring-boot:run ) >/tmp/auth.log 2>&1 &
AUTH_PID=$!
trap 'kill $ID_PID $AUTH_PID 2>/dev/null || true' EXIT

for i in $(seq 1 60); do
  if curl -fs http://127.0.0.1:8211/internal/id/next?bizTag=order >/dev/null 2>&1 \
     && curl -fs http://127.0.0.1:8201/internal/auth/health >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

say "5/5 self check"
ID_JSON=$(curl -fs http://127.0.0.1:8211/internal/id/next?bizTag=order)
echo "$ID_JSON" | grep -q '"id":' || fail "id-service response: $ID_JSON"

AUTH_JSON=$(curl -fs http://127.0.0.1:8201/internal/auth/health)
echo "$AUTH_JSON" | grep -q '"auth-ok"' || fail "auth-service response: $AUTH_JSON"

printf '\nPASS\n'