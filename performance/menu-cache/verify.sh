#!/bin/bash
# app 인스턴스에서 실행
#   diff         기존(user :8081, admin :8086)과 JOIN(user :8082, admin :8085)의 메뉴 목록 응답을 전 주점 비교
#   admin <포트>  그 admin 으로 전 주점 메뉴 목록을 한 번씩 조회 (쿼리 수 집계용)
T=$(curl -s -X POST localhost:8085/admin/users/login -H 'Content-Type: application/json' \
  -d '{"email":"load@test.com","password":"loadPass1!"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["response"]["body"]["accessToken"])')
user() { curl -s localhost:$1/v1/stores/LOAD$(printf %08d $2)/menus; }
admin() { curl -s localhost:$1/admin/menus/all-menus/stores/$2 -H "Authorization: Bearer $T"; }
case $1 in
  diff)
    for kind in user admin; do
      if [ $kind = user ]; then old=8081 new=8082; else old=8086 new=8085; fi
      same=0 diff=0
      for s in $(seq $STORES); do
        a=$($kind $old $s) b=$($kind $new $s)
        if [ "$a" = "$b" ] && [[ $a == *'"menuId"'* ]]; then same=$((same + 1)); continue; fi
        diff=$((diff + 1))
        [ $diff = 1 ] && printf '%s 주점 %s 다름\n기존: %.600s\nJOIN: %.600s\n' $kind $s "$a" "$b"
      done
      echo "$kind 메뉴 목록: 같음 $same / 다름 $diff"
    done ;;
  admin) for s in $(seq $STORES); do admin $2 $s > /dev/null; done ;;
esac
