# Product Stock Performance Test

## 환경
- Java 21
- PostgreSQL
- JMeter ...
- 테스트 대상 commit/tag

## 데이터 준비

- `data/product-stock-seed.sql`을 product-service DB에서 실행하면 테스트 전용 상품과 재고가 생성된다.
- Concurrency Test fixture `productId`: `33333333-3333-4333-8333-333333333333` (초기 재고 100개)
- UpdateStock Conflict Test fixture `productId`: `44444444-4444-4444-8444-444444444444` / `sellerId`: `dddddddd-dddd-4ddd-8ddd-dddddddddddd` (초기 재고 1,000개, 예약 없음)
- Isolated Reservation fixture `productId`: `55555555-5555-4555-8555-555555555555` / `sellerId`: `eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee` (재고 50개 중 20개가 `EXPIRATION_FAILED` 상태 예약(`reservationId`: `55555555-5555-4555-8555-555555555557`)에 물려 격리되어 있음, JMeter 테스트 대상이 아니라 격리 재고 조회/복구 API 확인용)
- 앞의 두 JMeter 테스트는 요청마다 `orderId`/`orderItemId`를 JMeter `__UUID()` 함수로 생성한다.
- JMeter 요청은 실제 DB 트랜잭션을 수행하므로 테스트 후 자동 롤백되지 않는다. 전용 상품·DB를 사용하고 성공한 예약과 재고 변동은 seed 재실행으로 정리한다(seed는 실행 전 이전 결과를 먼저 삭제한다).

모든 명령은 `product-service` 디렉터리를 현재 작업 디렉터리로 두고 실행한다.

```bash
cd product-service
mkdir -p test-results/product_stock
psql "$PRODUCT_DATABASE_URL" \
  -f performance-test/product_stock/data/product-stock-seed.sql
```

## 1. Concurrency Test (reserve)
`jmeter/concurrency-test.jmx`

일반재고 `reserve`는 타임딜과 달리 Redis 원자적 연산이 없고 JPA 낙관적 락(`version`) + `@Retryable`(최대 3회, 50ms 배수 백오프)만으로 동시성을 제어한다. 타임딜의 SyncTimer 500명 동시 발사 패턴을 그대로 가져오면 낙관적 락이 못 버티는 게 당연한 결과라 확인 실익이 없어, 기존에 측정하던 동시성 수준(50 Thread, 5초 Ramp-up)을 유지해 이전 측정치와 비교 가능하게 한다.

- 초기 재고: 100개
- 요청당 예약 수량: 1개
- Threads: 50
- Ramp-up: 5초
- Loop Count: 10
- 총 요청 수: 500회

요청 대상:

`POST http://localhost:8082/api/v1/internal/stocks/reserve`

(포트는 실행 환경에 맞게 `-JproductPort`로 넘긴다. 루트 `docker-compose.yml`로 전체 스택을 띄우면 product-service는 `${PRODUCT_PORT:-8082}`로 매핑되어 8082가 기본값이다. jmx 자체 기본값은 8080이므로 이 스택 기준으로 테스트할 때는 반드시 `-JproductPort=8082`를 넘긴다.)

헤더:

- `Content-Type: application/json`
- `X-Service-Key: local-dev-key`

본문:

`{"orderId":"${__UUID()}","items":[{"productId":"${productId}","orderItemId":"${__UUID()}","quantity":1}]}`

실행 예시:

```bash
jmeter -n -t performance-test/product_stock/jmeter/concurrency-test.jmx \
  -JproductId=33333333-3333-4333-8333-333333333333 \
  -JproductPort=8082 \
  -l test-results/product_stock/concurrency.jtl \
  -j test-results/product_stock/concurrency.log
```

목적:

재고 정합성 검증(성공 예약 건수는 초기 재고 100건을 넘을 수 없다) 및 `reserve`의 낙관적 락 재시도(`@Retryable`, `PRODUCT_STOCK_CONFLICT`) 동작 확인.

JTL 기반 HTML Report 생성:

```bash
jmeter -g test-results/product_stock/concurrency.jtl \
  -o test-results/product_stock/concurrency-report
```

`errorCode 라벨링` PostProcessor가 응답 코드(`PRODUCT_STOCK_CONFLICT`, `PRODUCT_STOCK_NOT_FOUND` 등)를 샘플러 라벨에 붙여주므로, Summary Report에서 요청 라벨별로 충돌/성공 비율을 바로 확인할 수 있다.

## 2. UpdateStock Conflict Test
`jmeter/update-stock-conflict-test.jmx`

- Threads: 10
- Ramp-up: 1초
- Loop: 10회
- 총 요청 수: 100회
- Synchronizing Timer: 10개 Thread, timeout 5초 (루프마다 반복 동기화)

요청 대상:

`PATCH http://localhost:8082/api/v1/stocks/{productId}`

(위 Concurrency Test와 동일하게, 루트 `docker-compose.yml`로 전체 스택을 띄운 환경에서는 `-JproductPort=8082`를 넘긴다.)

헤더:

- `Content-Type: application/json`
- `X-User-Id: ${sellerId}`
- `X-User-Role: SELLER`

본문:

`{"totalQuantity":150}`

실행 예시:

```bash
jmeter -n -t performance-test/product_stock/jmeter/update-stock-conflict-test.jmx \
  -JproductId=44444444-4444-4444-8444-444444444444 \
  -JsellerId=dddddddd-dddd-4ddd-8ddd-dddddddddddd \
  -JproductPort=8082 \
  -l test-results/product_stock/update-conflict.jtl \
  -j test-results/product_stock/update-conflict.log
```

목적:

동일 재고 row에 대한 동시 `updateStock` 요청에서 낙관적 락(version) 충돌이 재현되는지, `@Retryable`(`PRODUCT_STOCK_CONFLICT`, 최대 3회 재시도)이 충돌을 얼마나 흡수하는지 확인한다. fixture 재고는 예약이 없는 상태(`reservedQuantity = 0`)라 `totalQuantity` 값 자체의 유효성 검증에는 걸리지 않고, 순수하게 버전 충돌만 관찰할 수 있다.

JTL 기반 HTML Report 생성:

```bash
jmeter -g test-results/product_stock/update-conflict.jtl \
  -o test-results/product_stock/update-conflict-report
```

Report 출력 디렉터리는 기존에 존재하면 안 되므로 재생성할 때는 기존 Report 디렉터리를 비우거나 다른 이름을 사용한다.

## 3. 격리된 예약(EXPIRATION_FAILED) 확인
JMeter 시나리오가 아니라, seed로 만들어 둔 격리 상태를 API로 바로 확인하기 위한 fixture다. 만료 배치가 재고 복구(RESTORE)에 실패했을 때를 흉내 내, 재고 50개 중 20개가 예약에 물린 채(`available_quantity=30`) `EXPIRATION_FAILED` 상태로 남아 있다.

조회 (관리자 또는 해당 판매자):

```bash
curl http://localhost:8082/api/v1/stocks/reservations/isolated \
  -H "X-User-Id: eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee" \
  -H "X-User-Role: SELLER"
```

복구 (관리자만 가능):

```bash
curl -X POST http://localhost:8082/api/v1/stocks/reservations/55555555-5555-4555-8555-555555555557/recover \
  -H "X-User-Id: <관리자 UUID>" \
  -H "X-User-Role: ADMIN"
```

복구에 성공하면 예약이 `RESTORE` 이벤트로 정리되고 재고의 `available_quantity`가 50으로 돌아온다. 다시 격리 상태로 확인하려면 seed를 재실행한다(재실행 시 이전 예약·이벤트로그를 삭제하고 다시 만든다).
