#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

say() { printf '\n=== %s ===\n' "$1"; }
fail() { printf '\nFAIL: %s\n' "$1"; exit 1; }

say "1/4 mvn verify (reactor)"
./mvnw -q verify || fail "mvn verify"

say "2/4 common EventBus autoconfig test"
./mvnw -f minimall-common/pom.xml -q test -Dtest=EventBusAutoConfigurationTest \
    || fail "EventBus autoconfig test"

say "3/4 order EventBus dispatch (TDD green)"
./mvnw -f minimall-order-service/pom.xml -q test -Dtest=OrderServiceImplTest \
    || fail "OrderServiceImplTest"

say "4/4 notify existing listener still works (local dev path)"
./mvnw -f minimall-notify-service/pom.xml -q test -Dtest=OrderPaidListenerTest \
    || fail "OrderPaidListenerTest"

printf '\nPASS\n'
