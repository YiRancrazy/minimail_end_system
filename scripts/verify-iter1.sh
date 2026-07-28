#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

say() { printf '\n=== %s ===\n' "$1"; }
fail() { printf '\nFAIL: %s\n' "$1"; exit 1; }

say "1/4 mvn verify"
./mvnw -q verify || fail "mvn verify"

say "2/4 bring up auth-service"
pkill -9 -f "spring-boot:run" 2>/dev/null || true
sleep 2
( SPRING_PROFILES_ACTIVE=test ./mvnw -q -pl minimall-auth-service spring-boot:run ) > /tmp/auth.log 2>&1 &
AUTH_PID=$!
trap 'kill $AUTH_PID 2>/dev/null || true' EXIT

say "3/4 wait for auth-service :8201"
for i in $(seq 1 40); do
  if curl -fs http://127.0.0.1:8201/api/v1/auth/me -H "Authorization: Bearer x" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

say "4/4 auth round-trip"
USER="u$RANDOM"
REG=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"secret\"}" \
  http://127.0.0.1:8201/api/v1/auth/register)
echo "$REG" | grep -q '"accessToken":' || fail "register: $REG"
TOK=$(echo "$REG" | python -c "import sys,json;print(json.load(sys.stdin)['data']['accessToken'])")

LOGIN=$(curl -s -X POST -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER\",\"password\":\"secret\"}" \
  http://127.0.0.1:8201/api/v1/auth/login)
echo "$LOGIN" | grep -q '"accessToken":' || fail "login: $LOGIN"

ME=$(curl -s -H "Authorization: Bearer $TOK" \
  http://127.0.0.1:8201/api/v1/auth/me)
echo "$ME" | grep -q "\"username\":\"$USER\"" || fail "me: $ME"

INVALID=$(curl -s -H "Authorization: Bearer xxx.yyy.zzz" \
  http://127.0.0.1:8201/api/v1/auth/me)
echo "$INVALID" | grep -q '"14003"' || fail "expected token-invalid error code, got: $INVALID"

printf '\nPASS\n'