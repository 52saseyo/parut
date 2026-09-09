package com.parut.product.timedeal.domain.timedeal;

// NOTE: product의 AppearanceType과 값이 같지만 별도로 둔다 — 타임딜은 생성 시점 스냅샷이라 자기 완결적이어야 하고,
// 직접 등록 경로에는 참조할 상품이 아예 없다.
public enum TimeDealProductGrade {
    NORMAL,
    UGLY
}
