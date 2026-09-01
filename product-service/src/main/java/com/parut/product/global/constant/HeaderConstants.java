package com.parut.product.global.constant;
/**
 * Gateway가 하위 서비스로 전달하는 인증 정보 헤더 이름을 상수로 관리하는 클래스.
 * 게이트웨이가 JWT를 검증한 후, 그 결과를 이 헤더들에 담아 넘겨준다.
 * 서비스 코드 전반에서 "X-User-Id" 같은 문자열을 직접 타이핑하지 않고
 * 이 상수를 참조해서 오타/불일치를 방지한다.
 */
public final class HeaderConstants {

    // 요청자의 사용자 ID (UUID 문자열). AuditorAware에서 created_by/updated_by 채울 때 사용
    public static final String USER_ID = "X-User-Id";
    // 요청자의 권한(Role). external API 권한 체크 시 사용
    public static final String USER_ROLE = "X-User-Role";

    // 인스턴스화 방지 (유틸리티/상수 클래스 패턴)
    private HeaderConstants() {
    }
}