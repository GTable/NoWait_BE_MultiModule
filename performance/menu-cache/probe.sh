#!/bin/bash
# 품절 토글 → 손님 조회 반영까지. app 인스턴스에서 실행. 한 줄에: 토글 응답 ms, 토글 응답 후 반영 확인까지 ms, 그 사이 옛 값을 받은 조회 수
U=localhost:8081/v1/stores/LOAD00000001/menus/1
T=$(curl -s -X POST localhost:8085/admin/users/login -H 'Content-Type: application/json' \
  -d '{"email":"load@test.com","password":"loadPass1!"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["response"]["body"]["accessToken"])')
sold() { curl -s $U | grep -o '"isSoldOut":[a-z]*'; }
for i in $(seq ${N:-30}); do
  b=$(sold)
  t0=$(date +%s%3N)
  curl -s -o /dev/null -X PATCH localhost:8085/admin/menus/toggle-soldout/1 -H "Authorization: Bearer $T"
  t1=$(date +%s%3N)
  n=0
  while [ "$(sold)" = "$b" ] && [ $n -lt 1000 ]; do n=$((n + 1)); done
  t2=$(date +%s%3N)
  echo "$((t1 - t0)) $((t2 - t1)) $n"
  sleep 1
done
