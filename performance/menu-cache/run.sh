#!/bin/bash
# 메뉴 목록 조회 단계별 비교: 기존 N+1 → JOIN → JOIN+인덱스 → JOIN+인덱스+캐시. 결과는 performance/menu-cache/out/<이름>/
# 기본은 쿠폰 프로젝트의 EC2 3대(db·app·k6, ~/Desktop/CouponToyProject/performance/ec2/README.md)를 Mac 에서 SSH 로만 다룬다
# LOCAL=1 이면 Mac 한 대: MySQL·Redis 는 도커(EC2 db 처럼 각 1 CPU·1GB), 앱·k6 는 Mac. CPU 를 나눠 쓰니 절대값보다 단계 간 차이를 본다
#   JARS=<user-asis·user-tobe·admin-asis·admin.jar 디렉터리> run.sh setup   jar·스크립트 올리기, 스키마·목업 데이터 만들기
#   run.sh verify          기존 코드와 JOIN 코드의 메뉴 목록 응답 비교(user·admin, 전 주점) + admin 목록 1회당 쿼리 수
#   run.sh bench <단계>...  nplus1 | join | join-idx | cache. RATES 단계마다 측정 (손님 목록 조회 + 초당 WRITE_RATE 건 품절 토글)
#   run.sh delay           부하 중 품절 토글 → 손님 조회 반영까지 걸린 시간 (cache)
#   run.sh cleanup         앱 종료, nowait_lab 스키마 삭제 (LOCAL 은 컨테이너까지)
#     STORES=200  RATES="200 500 1000"  DURATION=60s  WRITE_RATE=1  LIST_RATIO=1  DELAY_RATE=400
set -u
cd "$(dirname "$0")/../.."
D=performance/menu-cache
STORES=${STORES:-200}
IDX=idx_menus_store_id_deleted_sort_order  # Menu 엔티티의 @Index 와 같게

if [ "${LOCAL:-}" ]; then
  WORK=$D/out/local; mkdir -p $WORK/logs
  DB_PUB=local APP_PUB=local K6_PUB=local DB_PRIV=127.0.0.1 APP_PRIV=127.0.0.1 DB_PORT=13306 REDIS_PORT=16379 DB_PASSWORD=bench
  MYSQL="docker exec -i -e MYSQL_PWD=$DB_PASSWORD nowait-bench-mysql mysql -uroot -h127.0.0.1"
  on() { shift; (cd $WORK && bash -c "$*"); }
  put() { shift; local f; for f in "${@:1:$#-1}"; do ln -sf "$(realpath "$f")" $WORK/; done; }
  get() { cp $WORK/$2 $3; }
  q() { $MYSQL -N nowait_lab -e "$1"; }
  sqlf() { $MYSQL $1; }  # 표준 입력의 SQL 실행
  mysql_usec() { docker exec nowait-bench-mysql cat /sys/fs/cgroup/cpu.stat | awk '$1=="usage_usec"{print $2}'; }
else
  source ${HOSTS:-$HOME/Desktop/CouponToyProject/build/ec2/hosts.env}
  DB_PORT=3306 REDIS_PORT=6379
  SSH_OPTS="-i $KEY -o StrictHostKeyChecking=accept-new -o LogLevel=ERROR -o ControlMaster=auto -o ControlPath=$HOME/.ssh/cm-%r@%h:%p -o ControlPersist=10m"
  on() { local h=$1; shift; ssh $SSH_OPTS ubuntu@$h "$@"; }
  put() { local h=$1; shift; scp -q $SSH_OPTS "${@:1:$#-1}" ubuntu@$h:"${@: -1}"; }
  get() { scp -q $SSH_OPTS ubuntu@$1:"$2" "$3"; }
  q() { on $DB_PUB "mysql -N nowait_lab -e \"$1\""; }
  sqlf() { on $DB_PUB "mysql $1"; }
  mysql_usec() { on $DB_PUB 'cat /sys/fs/cgroup/system.slice/docker-$(docker inspect -f "{{.Id}}" coupon-toy-mysql).scope/cpu.stat' | awk '$1=="usage_usec"{print $2}'; }
fi
now() { python3 -c 'import time; print(time.time())'; }

# 운영 시작 스크립트와 같은 힙. SQL 로그는 끄고, 에러 알림이 팀 Discord 로 가지 않게 웹훅을 막는다
APP_ARGS="--spring.datasource.url='jdbc:mysql://$DB_PRIV:$DB_PORT/nowait_lab?createDatabaseIfNotExist=true&serverTimezone=Asia/Seoul' \
  --spring.datasource.username=root --spring.datasource.password=$DB_PASSWORD \
  --spring.data.redis.host=$DB_PRIV --spring.data.redis.port=$REDIS_PORT --spring.data.redis.password= \
  --spring.jpa.show-sql=false --spring.jpa.properties.hibernate.show_sql=false \
  --discord.webhook.user-url=http://127.0.0.1:9 --discord.webhook.admin-url=http://127.0.0.1:9"
start() { # start <jar> <포트> <프로필> [추가 인자...]
  local jar=$1 port=$2 profile=$3; shift 3
  on $APP_PUB "nohup java -Xms256m -Xmx512m -XX:+UseG1GC -Duser.timezone=Asia/Seoul -jar $jar --server.port=$port \
    --spring.profiles.active=$profile $APP_ARGS $* > logs/${jar%.jar}.log 2>&1 < /dev/null &"
  local i; for i in $(seq 1 120); do on $APP_PUB "curl -sf -m 2 localhost:$port/health-check" > /dev/null && return 0; sleep 1; done
  echo "$jar did not start"; on $APP_PUB "tail -30 logs/${jar%.jar}.log"; return 1
}
# pkill·pgrep 패턴은 [j] [.] 로 써서 그 명령을 돌리는 원격 셸 자신과는 맞지 않게 한다
running() { on $APP_PUB "pgrep -f '[j]ava .*${1%.jar}[.]jar' > /dev/null"; }
stop() { on $APP_PUB "pkill -f '[j]ava .*${1%.jar}[.]jar'; for i in \$(seq 1 30); do pgrep -f '[j]ava .*${1%.jar}[.]jar' > /dev/null || exit 0; sleep 1; done"; }
cpu_time() { on $APP_PUB "ps -o time= -p \$(pgrep -f '[j]ava .*${1%.jar}[.]jar')" | tr -d ' '; }  # 그 JVM 의 누적 CPU 시간

cache_stats() { # hit<TAB>miss (캐시 없는 기존 코드는 빈 값)
  on $APP_PUB "curl -s localhost:8081/nowait/management/prometheus" | awk '/^cache_gets_total\{/ && /cache="cache:menus"/ {
    if ($0 ~ /result="hit"/) h=$2; else if ($0 ~ /result="miss"/) m=$2 } END { printf "%s\t%s", h, m }'
}
index() { # index on|off  앱(ddl-auto)이 만든 인덱스를 단계에 맞게 지우거나 다시 만든다
  q "DROP INDEX $IDX ON menus" 2>/dev/null
  [ $1 = off ] || q "CREATE INDEX $IDX ON menus (store_id, deleted, sort_order)"
}

setup() {
  set -e
  local J=${JARS:?JARS=<jar 디렉터리>}
  if [ "${LOCAL:-}" ]; then
    docker rm -f nowait-bench-mysql nowait-bench-redis > /dev/null 2>&1 || true
    docker run -d --name nowait-bench-mysql --cpus=1 -m 1g -e MYSQL_ROOT_PASSWORD=$DB_PASSWORD -p $DB_PORT:3306 mysql:8.0.36 > /dev/null
    docker run -d --name nowait-bench-redis --cpus=1 -m 1g -p $REDIS_PORT:6379 redis:7.2-alpine > /dev/null
    until $MYSQL -e 'SELECT 1' > /dev/null 2>&1; do sleep 2; done
  else
    on $APP_PUB "command -v java >/dev/null || { sudo apt-get update -q && sudo apt-get install -yq openjdk-17-jre-headless; }; mkdir -p logs"
  fi
  put $APP_PUB $J/user-asis.jar $J/user-tobe.jar $J/admin-asis.jar $J/admin.jar $D/probe.sh $D/verify.sh .
  put $K6_PUB $D/menu-load.js .
  # 스키마는 앱이 만든다(ddl-auto: update). 두 앱이 동시에 만들면 FK 이름이 겹치므로 차례로 띄운다
  stop user-asis.jar; stop user-tobe.jar; stop admin-asis.jar; stop admin.jar
  start user-tobe.jar 8081 user-local
  start admin.jar 8085 admin-local
  { echo "SET @stores = $STORES;"; cat $D/seed.sql; } | sqlf nowait_lab
  on $APP_PUB "curl -s -X POST localhost:8085/admin/users/signup -H 'Content-Type: application/json' \
    -d '{\"email\":\"load@test.com\",\"password\":\"loadPass1!\",\"nickname\":\"loadtester\",\"socialType\":\"LOCAL\"}'"; echo
  q "UPDATE users SET role = 'SUPER_ADMIN' WHERE email = 'load@test.com'"  # 모든 주점의 admin 메뉴 목록을 볼 수 있게
  echo "stores menus menu_images users: $(q "SELECT (SELECT COUNT(*) FROM stores), (SELECT COUNT(*) FROM menus), (SELECT COUNT(*) FROM menu_images), (SELECT COUNT(*) FROM users)")"
  stop user-tobe.jar
}

verify() {
  L=$D/out/verify; mkdir -p $L
  running admin.jar || start admin.jar 8085 admin-local || return 1
  stop user-asis.jar; stop user-tobe.jar; stop admin-asis.jar
  start user-asis.jar 8081 user-local && start user-tobe.jar 8082 user-local && start admin-asis.jar 8086 admin-local || return 1
  on $APP_PUB "STORES=$STORES bash verify.sh diff" | tee $L/diff.txt
  local port; for port in 8086 8085; do
    q "TRUNCATE TABLE performance_schema.events_statements_summary_by_digest"
    on $APP_PUB "STORES=$STORES bash verify.sh admin $port"
    echo "admin :$port 메뉴 목록 $STORES 회 — $(sqlf "-N nowait_lab" < $D/digest.sql | awk '{printf "%s %s건 ", $1, $2}')"
  done | tee $L/admin-queries.txt
  stop user-asis.jar; stop user-tobe.jar; stop admin-asis.jar
}

level() { # level <이름> <초당 요청> <시간>
  local name=$1 rate=$2 dur=$3
  q "TRUNCATE TABLE performance_schema.events_statements_summary_by_digest"
  local c0=$(cache_stats) m0=$(mysql_usec) p0=$(cpu_time $JAR) t0=$(now)
  on $K6_PUB "env USER_URL=http://$APP_PRIV:8081 ADMIN_URL=http://$APP_PRIV:8085 STORES=$STORES RATE=$rate DURATION=$dur \
    WRITE_RATE=${WRITE_RATE:-1} LIST_RATIO=${LIST_RATIO:-1} k6 run --quiet --no-color --summary-export k6-$name.json menu-load.js" > $L/$name.log 2>&1
  local ec=$? t1=$(now) c1=$(cache_stats) m1=$(mysql_usec) p1=$(cpu_time $JAR)
  sqlf "-N nowait_lab" < $D/digest.sql > $L/$name.digest
  get $K6_PUB k6-$name.json $L/$name.json
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' $name $rate "$c0" "$c1" $((m1 - m0)) $(python3 -c "print($t1 - $t0)") $p0 $p1 >> $L/runs.tsv
  echo "$name k6_exit=$ec"
}

bench() {
  local cfg rate
  for cfg in "$@"; do
    local ttl= idx=on; JAR=user-tobe.jar
    case $cfg in
      nplus1) JAR=user-asis.jar idx=off ;;
      join) ttl=0s idx=off ;;
      join-idx) ttl=0s ;;
      cache) ;;
      *) echo "모르는 단계: $cfg"; return 1 ;;
    esac
    L=$D/out/$cfg; mkdir -p $L; : > $L/runs.tsv
    running admin.jar || start admin.jar 8085 admin-local || return 1
    stop user-asis.jar; stop user-tobe.jar
    q "UPDATE menus SET is_sold_out = 0"
    start $JAR 8081 user-local ${ttl:+--nowait.cache.menu.ttl=$ttl} || return 1
    index $idx
    level warm-$cfg 100 20s  # 예열 (보고서에서 뺀다)
    for rate in ${RATES:-200 500 1000}; do level $cfg-$rate $rate ${DURATION:-60s}; done
    stop $JAR
    report $L | tee $L/report.md
  done
}

report() {
  python3 - "$1" <<'EOF'
import json, sys
L = sys.argv[1]
def secs(t):  # ps 의 time: [D-]HH:MM:SS (Linux), [H:]M:SS.ss (macOS)
    d, _, hms = t.rpartition('-')
    s = 0.0
    for x in hms.split(':'):
        s = s * 60 + float(x)
    return s + int(d or 0) * 86400
print('| 목표 req/s | 처리 req/s | p50 ms | p95 ms | p99 ms | 실패 | 못 보낸 요청 | 히트율 | stores | menus | menu_images | 요청당 쿼리 | 요청당 읽은 행 | MySQL CPU(코어) | user-api CPU(코어) |')
print('|' + '---|' * 15)
for line in open(f'{L}/runs.tsv'):
    name, rate, h0, m0, h1, m1, mus, el, p0, p1 = line.rstrip('\n').split('\t')
    if name.startswith('warm'):
        continue
    k = json.load(open(f'{L}/{name}.json'))['metrics']
    d, n = k['http_req_duration{scenario:reads}'], k['http_reqs{scenario:reads}']
    failed = k['http_req_failed{scenario:reads}'].get('value', 0)
    dropped = k.get('dropped_iterations', {}).get('count', 0)
    q = {t: (int(c), int(r)) for t, c, r, _ in (l.split('\t') for l in open(f'{L}/{name}.digest') if l.strip())}
    hit, miss = float(h1 or 0) - float(h0 or 0), float(m1 or 0) - float(m0 or 0)
    hr = f'{hit / (hit + miss) * 100:.1f}%' if hit + miss else '-'
    calls = {t: q.get(t, (0, 0))[0] for t in ('stores', 'menus', 'menu_images')}
    rows = sum(q.get(t, (0, 0))[1] for t in ('stores', 'menus', 'menu_images'))
    el = float(el)
    print(f"| {rate} | {n['rate']:.0f} | {d['med']:.1f} | {d['p(95)']:.1f} | {d['p(99)']:.1f} | {failed * 100:.2f}% | {dropped:.0f} "
          f"| {hr} | {calls['stores']} | {calls['menus']} | {calls['menu_images']} | {sum(calls.values()) / n['count']:.2f} "
          f"| {rows / n['count']:.0f} | {int(mus) / 1e6 / el:.2f} | {(secs(p1) - secs(p0)) / el:.2f} |")
EOF
}

delay() {
  L=$D/out/delay; mkdir -p $L
  running admin.jar || start admin.jar 8085 admin-local || return 1
  stop user-asis.jar
  running user-tobe.jar || start user-tobe.jar 8081 user-local || return 1
  index on
  on $K6_PUB "nohup env USER_URL=http://$APP_PRIV:8081 ADMIN_URL=http://$APP_PRIV:8085 STORES=$STORES RATE=${DELAY_RATE:-400} DURATION=120s \
    WRITE_RATE=1 LIST_RATIO=${LIST_RATIO:-0.8} k6 run --quiet --no-color menu-load.js > delay-k6.log 2>&1 < /dev/null &"
  sleep 15
  on $APP_PUB "N=${N:-30} bash probe.sh" | tee $L/probe.txt
  on $K6_PUB "while pgrep -x k6 > /dev/null; do sleep 1; done"
  python3 - $L/probe.txt <<'EOF' | tee $L/report.md
import statistics, sys
rows = [list(map(int, l.split())) for l in open(sys.argv[1]) if l.strip()]
toggle, reflect, stale = zip(*rows)
p = lambda xs, q: sorted(xs)[min(len(xs) - 1, int(len(xs) * q))]
print(f'토글 {len(rows)}회 | 토글 응답 p50 {p(toggle, .5)}ms p95 {p(toggle, .95)}ms '
      f'| 응답 후 반영 확인 p50 {p(reflect, .5)}ms p95 {p(reflect, .95)}ms 최대 {max(reflect)}ms '
      f'| 옛 값을 받은 조회 {sum(stale)}회')
EOF
}

cleanup() {
  stop user-asis.jar; stop user-tobe.jar; stop admin-asis.jar; stop admin.jar
  if [ "${LOCAL:-}" ]; then docker rm -f nowait-bench-mysql nowait-bench-redis > /dev/null; else q "DROP DATABASE IF EXISTS nowait_lab"; fi
}

case ${1:-} in
  setup) setup ;;
  verify) verify ;;
  bench) shift; bench "$@" ;;
  delay) delay ;;
  report) report "$2" ;;
  cleanup) cleanup ;;
  *) sed -n 2,10p "$0"; exit 1 ;;
esac
