# TimeDeal Performance Test

## 환경
- Java 21
- PostgreSQL
- Redis
- JMeter ...
- 테스트 대상 commit/tag

## 데이터 준비

- 테스트 대상 타임딜을 ACTIVE 상태로 준비한다.
- Concurrency Test 대상은 초기 판매 가능 재고 100개로 설정한다.
- Load Test 대상은 총 1,000건의 요청보다 충분히 큰 재고를 별도로 설정한다.
- `data/time-deal-seed.sql`을 product-service DB에서 실행하면 테스트 전용 타임딜과 재고가 생성된다.
- Concurrency fixture `timeDealId`: `11111111-1111-4111-8111-111111111111` (초기 재고 100개)
- Load fixture `timeDealId`: `22222222-2222-4222-8222-222222222222` (초기 재고 100,000개)
- 두 테스트 모두 요청마다 `userId`와 `orderId`를 JMeter `__UUID()` 함수로 생성한다. CSV EOF나 사용자별 최대 구매 수량에 걸리지 않도록 고정 사용자·주문 데이터를 사용하지 않는다.
- JMeter 요청은 실제 DB 트랜잭션을 수행하므로 테스트 후 자동 롤백되지 않는다. 전용 타임딜·DB를 사용하고 성공한 구매와 차감 재고를 별도 정리한다.

모든 명령은 `product-service` 디렉터리를 현재 작업 디렉터리로 두고 실행한다.

```bash
cd product-service
mkdir -p test-results
psql "$PRODUCT_DATABASE_URL" \
  -f performance-test/timedeal/data/time-deal-seed.sql
```

## 1. Concurrency Test
jmeter/concurrency-test.jmx

- 초기 타임딜 재고: 100개
- 요청당 구매 수량: 1개
- Threads: 500
- Ramp-up: 1초
- Loop Count: 1
- Synchronizing Timer: 500개 Thread, timeout 10초
- 총 요청 수: 500회

요청 대상:

`POST http://localhost:8080/api/v1/internal/time-deals/{timeDealId}/purchases`

헤더:

- `Content-Type: application/json`
- `X-Service-Key: local-dev-key`
- `X-User-Id: ${__UUID()}`

본문:

`{"orderId":"${__UUID()}","quantity":1}`

실행 예시:

```bash
jmeter -n -t performance-test/timedeal/jmeter/concurrency-test.jmx \
  -JtimeDealId=11111111-1111-4111-8111-111111111111 \
  -l test-results/concurrency-before.jtl \
  -j test-results/concurrency-before.log
```

목적:
재고 정합성 및 초과 판매 검증

JTL 기반 HTML Report 생성:

```bash
jmeter -g test-results/concurrency-before.jtl \
  -o test-results/concurrency-before-report
```

생성된 `test-results/concurrency-before-report/index.html`에서 Response Time Percentiles의 P95/P99를 확인한다. `jmeter.log`는 실행 과정 로그이므로 P95/P99 산출용 결과 파일로 사용하지 않는다.

## 2. Load Test
`jmeter/load-test.jmx`

- Threads: 100
- Ramp-up: 1초
- Loop: 10회
- 총 요청 수: 1,000회
- Duration: 사용하지 않음
- Synchronizing Timer: 사용하지 않음
- 사용자 ID: 요청마다 `__UUID()`로 새로 생성
- 주문 ID: 요청마다 `__UUID()`로 새로 생성

요청 대상:

`POST http://localhost:8080/api/v1/internal/time-deals/{timeDealId}/purchases`

측정:

- Throughput (TPS)
- Average response time
- P95 / P99
- Error Rate

목표:

- 200 TPS 이상을 안정적으로 처리하는지 확인
- Thread 수를 증가시키며 실제 병목과 처리 한계를 탐색

재고 설정:

- 총 1,000건의 요청을 모두 성공시키려면 초기 재고가 최소 1,000개 이상이어야 한다. 현재 seed는 여유를 두고 100,000개를 설정한다.
- 초기 재고를 100개로 두면 약 100건 성공 후 대부분의 요청이 재고 부족으로 실패하므로 Avg/Throughput/Success Rate 측정이 왜곡된다.
- 재고 정합성과 초과 판매 검증은 `concurrency-test.jmx`에서 초기 재고 100개로 별도 수행한다.

100개의 Thread가 각각 10회 요청하므로 총 1,000건을 측정한다. 요청마다 사용자 UUID를 새로 생성하므로 이를 실제 사용자 100명의 누적 구매량으로 해석하지 않는다.

실행 예시:

```bash
jmeter -n -t performance-test/timedeal/jmeter/load-test.jmx \
  -JtimeDealId=22222222-2222-4222-8222-222222222222 \
  -JproductPort=8080 \
  -l test-results/load.jtl \
  -j test-results/load.log
```

목적:

정합성 문제가 아니라 동일한 1,000건 배치 조건에서 Avg Response Time, Throughput, Success Rate, P95/P99를 측정한다. 측정 결과로 병목을 찾고 개선한 뒤 동일 조건으로 재측정한다.

JTL 기반 HTML Report 생성:

```bash
jmeter -g test-results/load.jtl \
  -o test-results/load-report
```

생성된 `test-results/load-report/index.html`에서 Average, Throughput, Success/Errors, P95/P99를 확인한다. Report 출력 디렉터리는 기존에 존재하면 안 되므로 재생성할 때는 기존 Report 디렉터리를 비우거나 다른 이름을 사용한다.
