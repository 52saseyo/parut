package com.parut.user.user.infrastructure;

import com.parut.user.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    // @SQLRestriction이 적용되어 있으므로 findById 호출 시 자동으로 deleted_at IS NULL 조건이 붙습니다.

    // 아이디(username) 또는 이름(name)에 검색어가 포함된 유저를 페이징하여 반환
    Page<User> findByUsernameContainingOrNameContaining(String username, String name, Pageable pageable);

    // 1. 회원가입 시 아이디 중복 체크용 (추가)
    boolean existsByUsername(String username);

    // 2. 로그인 시 회원 정보 조회용 (추가)
    Optional<User> findByUsername(String username);
}