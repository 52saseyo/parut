# 파릇 API 컬렉션 (Postman)

각자 작성한 엔드포인트를 팀원 누구나 바로 호출해볼 수 있도록, 전체 서비스의 API를 포스트맨 컬렉션 하나로 관리한다.

| 파일 | 역할 | 커밋 |
|---|---|---|
| `parut.postman_collection.json` | 요청 구조(폴더·URL·헤더·본문). Collection Format v2.1.0 | O — 이게 팀 공유 대상 |
| `local.postman_environment.json` | 값 서식(baseUrl, 시크릿, 테스트용 UUID) | O — **값은 비운 상태로만** |

## 1. 임포트

포스트맨 `Import` → 두 파일을 **한꺼번에** 드래그(또는 이 폴더를 통째로 드래그).

임포트 창구는 하나지만 포스트맨이 파일 내용을 보고 알아서 갈라 넣는다.

| 파일이 가진 키 | 들어가는 곳 |
|---|---|
| `info.schema: ".../collection/v2.1.0/..."` | 왼쪽 **Collections** 탭 |
| `_postman_variable_scope: "environment"` | 왼쪽 **Environments** 탭 |

임포트 후 **오른쪽 위 환경 드롭다운에서 `parut-local`을 선택**해야 `{{...}}`가 해석된다. 선택하지 않으면 컬렉션 변수만 살아 있어서 `{{productBaseUrl}}`은 동작하고 나머지는 `unresolved variable`로 빨갛게 뜬다.

## 2. 환경 값 채우기

임포트한 뒤 포스트맨 안에서 `parut-local` 환경을 열고 `CURRENT VALUE`를 채운다.

| 변수 | 채울 값 |
|---|---|
| `productBaseUrl` | 기본 `http://localhost:8080` |
| `orderBaseUrl` / `userBaseUrl` / `notificationBaseUrl` | 각 서비스 담당자가 포트 확정 후 공유 |
| `serviceKey` | 각 서비스 `.env`의 `INTERNAL_SERVICE_KEY` 값 |
| `userId` / `productId` / `timeDealId` / `orderId` / `orderItemId` | 본인 로컬 DB에 있는 테스트 데이터 UUID |

**`serviceKey`는 절대 커밋하지 않는다.** 내부 서비스 인증용 공유 시크릿이라 git 히스토리에 한 번 올라가면 지워도 남는다.

포스트맨은 임포트 시점에 파일 내용을 **복사**하고 이후 파일과 동기화하지 않는다. 그래서 앱에서 값을 채워도 커밋된 `local.postman_environment.json`은 빈 상태로 유지된다 — 값을 채운 상태로 export해서 덮어쓰지만 않으면 된다.

## 3. 폴더 규칙

```
{서비스} > {컨텍스트} > {internal | external} > {기능 요청}
```

- **컨텍스트 폴더 이름은 실제 패키지와 같게 맞춘다.** `order-service`의 `cart`/`order`/`payment`/`delivery`/`refund`/`settlement`는 `com.parut.order` 하위 패키지와 1:1이다. 폴더가 패키지와 어긋나면 "이 API를 어느 폴더에 넣지"를 매번 판단해야 한다.
- 컨텍스트 분리가 아직 없는 서비스(`user-service`, `notification-service`)는 모듈 폴더를 두지 않고 `internal`/`external`을 바로 둔다. 없는 구조를 미리 만들지 않는다.
- 나열 순서는 알파벳순이 아니라 **도메인 흐름 순서**다 (`cart → order → payment → delivery → refund → settlement`).

| 폴더 | 경로 | 인증 |
|---|---|---|
| `internal` | `/api/v1/internal/**` | `X-Service-Key`. **폴더 레벨 auth로 이미 걸려 있어 하위 요청에 자동 상속된다** — 요청마다 헤더를 넣지 않는다 |
| `external` | `/api/v1/**` | 없음. 필요하면 `X-User-Id` 헤더를 요청에 직접 추가 |

폴더가 4단까지 깊어서 클릭으로 찾기 번거로우면 포스트맨 상단 검색바로 요청 이름을 바로 찾는 게 빠르다. 그래서 **요청 이름은 폴더 맥락 없이도 구분되게** 짓는다 (`재고 선점`, `타임딜 구매 예약`).

## 4. 새 API를 추가할 때

1. 포스트맨에서 자기 컨텍스트의 `internal` / `external` 폴더 안에 요청을 추가한다.
2. URL은 `{{productBaseUrl}}`처럼 **서비스별 baseUrl 변수**로 시작한다. 호스트를 직접 적지 않는다.
3. 경로 변수와 식별자는 `{{productId}}`처럼 환경 변수로 둔다. 실제 UUID를 박아 넣으면 다른 사람 로컬에서 동작하지 않는다.
4. 요청 `description`에 성공 응답 코드와 주요 실패 조건을 한 줄 적는다.
5. 컬렉션을 `Export`(Collection v2.1) 하고, 아래 정리를 거쳐 `parut.postman_collection.json`을 덮어쓴 뒤 커밋한다.

### export 후 커밋 전에 지울 것

| 지울 것 | 이유 |
|---|---|
| `_postman_id`, `_exporter_id`, `_collection_link` | export할 때마다 바뀌고 사람마다 달라서, 내용 변화가 없어도 diff가 뜨고 머지 충돌이 난다 |
| 각 요청의 `response` 배열 (저장된 응답 예시) | 파일 용량 대부분을 차지하고 실제 데이터가 섞여 들어간다 |

이 저장소는 모노레포이므로 네 서비스 담당자가 **같은 파일**을 고친다. 자기 컨텍스트 폴더 밖은 건드리지 않는 것을 원칙으로 하고, 남이 추가한 요청이 export 과정에서 사라지지 않았는지 diff로 확인한다.

## 5. 다시 임포트할 때

컬렉션이 갱신되면 다시 임포트해야 한다. 이름이 같으면 포스트맨이 `Replace existing` / `Create new`를 물어보는데 **Replace**를 고른다. 임포트는 파일과의 링크가 아니라 복사이므로, 앱에 남아 있는 예전 버전은 자동으로 갱신되지 않는다.

## 6. 참고

- 손으로 관리하기 번거로워지면 `springdoc-openapi-starter-webmvc-ui`를 붙여 `/v3/api-docs`를 포스트맨에 직접 임포트하는 방법도 있다. 컨트롤러 시그니처에서 자동 생성되니 파일 유지보수가 없어지는 대신, 요청 본문 예시값은 손으로 넣은 것만큼 나오지 않는다.
- 컬렉션 상세 규칙은 `parut.postman_collection.json`의 `info.description`에도 요약해 두었다.
