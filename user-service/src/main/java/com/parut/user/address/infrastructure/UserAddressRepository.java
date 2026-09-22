package com.parut.user.address.infrastructure;

import com.parut.user.address.domain.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    // @SQLRestriction이 적용되어 있으므로 아래 조회는 자동으로 deleted_at IS NULL 조건이 붙습니다.
    // createdAt은 애플리케이션이 생성하므로 같은 값이 나올 수 있어, 순서가 필요한 조회는 id로 보조 정렬합니다.

    // 1. 목록 조회 (기본 배송지 우선, 이후 최근 등록 순)
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDescIdDesc(UUID userId);

    // 2. 본인 소유 배송지 단건 조회
    Optional<UserAddress> findByIdAndUserId(UUID id, UUID userId);

    // 3. 현재 기본 배송지 조회
    Optional<UserAddress> findFirstByUserIdAndIsDefaultTrue(UUID userId);

    // 4. 기본 배송지 삭제 후 새 기본 배송지로 지정할 최근 배송지 조회
    Optional<UserAddress> findFirstByUserIdOrderByCreatedAtDescIdDesc(UUID userId);

    // 5. 첫 배송지 여부 확인
    boolean existsByUserId(UUID userId);
}
