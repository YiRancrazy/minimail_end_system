#!/usr/bin/env bash
# Iter-5 lint: forbid raw BizException(String code, ...) — must use *CodeEnum.
# Exit 1 on any hit.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"

hit=$(grep -rE 'new BizException\("' \
  --include="*.java" \
  minimall-*/src/main/java/com/yirancrazy/minimall/ 2>/dev/null | wc -l)

if [ "$hit" -ne 0 ]; then
  echo "FAIL: $hit raw BizException(\"...\") calls remain. Use *CodeEnum."
  grep -rnE 'new BizException\("' --include="*.java" minimall-*/src/main/java/com/yirancrazy/minimall/
  exit 1
fi

echo "OK: zero literal BizException calls. All errors via *CodeEnum."
exit 0