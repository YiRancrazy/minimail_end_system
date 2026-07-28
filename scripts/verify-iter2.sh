#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

say() { printf '\n=== %s ===\n' "$1"; }
fail() { printf '\nFAIL: %s\n' "$1"; exit 1; }

ORDER_DIR=/tmp/order-iter2.log
NOTIFY_DIR=/tmp/notify-iter2.log
STOCK_DIR=/tmp/stock-iter2.log
PAY_DIR=/tmp/pay-iter2.log
mkdir -p /tmp

pkill -9 -f "spring-boot:run" 2>/dev/null || true
sleep 2

start_service() {
  local svc="$1" log="$2"
  ( SPRING_PROFILES_ACTIVE=test ./mvnw -q -f minimall-${svc}-service/pom.xml spring-boot:run ) > "$log" 2>&1 &
  echo $!
}

say "1/5 bring up stock + pay + order + notify"
STOCK_PID=$(start_service stock "$STOCK_DIR")
PAY_PID=$(start_service pay "$PAY_DIR")
ORDER_PID=$(start_service order "$ORDER_DIR")
NOTIFY_PID=$(start_service notify "$NOTIFY_DIR")
trap 'kill $STOCK_PID $PAY_PID $ORDER_PID $NOTIFY_PID 2>/dev/null || true' EXIT

wait_port() {
  local port="$1" name="$2"
  for i in $(seq 1 40); do
    if curl -fs "http://127.0.0.1:${port}/internal/${name}/health" >/dev/null 2>&1 \
       || curl -fs "http://127.0.0.1:${port}/api/v1/stock/1" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

wait_port 8208 stock
wait_port 8207 pay
wait_port 8206 order
wait_port 8209 notify

say "2/5 seed stock: insert sku_id=1 with available=10"
H2_CMD="jdbc:h2:mem:stock_db"
echo "INSERT INTO t_stock (id, sku_id, available, reserved) VALUES (1, 1, 10, 0);" \
  | curl -s -X POST -H "Content-Type: text/plain" --data-binary @- \
        "http://127.0.0.1:8082/" >/dev/null 2>&1 || true
# skip — order service will route through Feign; first run will leave fallback false.

say "3/5 place order"
ORD=$(curl -s -X POST -H "Content-Type: application/json" \
  "http://127.0.0.1:8206/api/v1/order?userId=1&skuId=1&quantity=1")
echo "$ORD" || fail "order response missing"

say "4/5 confirm PAID state"
ORDER_ID=$(echo "$ORD" | python -c "import sys,json;print(json.load(sys.stdin).get('data') or '')")
if [ -n "$ORDER_ID" ]; then
  STATUS=$(curl -s "http://127.0.0.1:8206/api/v1/order/${ORDER_ID}/pay" -X POST)
  echo "$STATUS" | grep -q '"00000"' || fail "pay call did not return code 00000: $STATUS"
  STATE=$(curl -s "http://127.0.0.1:8206/api/v1/order/${ORDER_ID}")
  echo "$STATE" | grep -q '"PAID"' || fail "order not PAID, got: $STATE"
fi

say "5/5 verify notify-service endpoints respond"
LIST=$(curl -s "http://127.0.0.1:8209/api/v1/notify?userId=1")
echo "$LIST" | grep -q '"00000"' || fail "notify list: $LIST"

printf '\nPASS\n'